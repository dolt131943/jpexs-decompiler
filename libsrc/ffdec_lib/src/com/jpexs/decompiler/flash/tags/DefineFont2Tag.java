/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.helpers.FontHelper;
import com.jpexs.decompiler.flash.tags.base.FontTag;
import com.jpexs.decompiler.flash.types.BasicType;
import com.jpexs.decompiler.flash.types.KERNINGRECORD;
import com.jpexs.decompiler.flash.types.LANGCODE;
import com.jpexs.decompiler.flash.types.RECT;
import com.jpexs.decompiler.flash.types.SHAPE;
import com.jpexs.decompiler.flash.types.annotations.Conditional;
import com.jpexs.decompiler.flash.types.annotations.SWFType;
import com.jpexs.decompiler.flash.types.shaperecords.SHAPERECORD;
import com.jpexs.helpers.ByteArrayRange;
import com.jpexs.helpers.utf8.Utf8Helper;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author JPEXS
 */
public class DefineFont2Tag extends FontTag {

    @SWFType(BasicType.UI16)
    public int fontId;

    public boolean fontFlagsHasLayout;

    public boolean fontFlagsShiftJIS;

    public boolean fontFlagsSmallText;

    public boolean fontFlagsANSI;

    public boolean fontFlagsWideOffsets;

    public boolean fontFlagsWideCodes;

    public boolean fontFlagsItalic;

    public boolean fontFlagsBold;

    public LANGCODE languageCode;

    public String fontName;

    public List<SHAPE> glyphShapeTable;

    @SWFType(value = BasicType.UI8, alternateValue = BasicType.UI16, alternateCondition = "fontFlagsWideCodes")
    public List<Integer> codeTable;

    @SWFType(BasicType.UI16)
    @Conditional("fontFlagsHasLayout")
    public int fontAscent;

    @SWFType(BasicType.UI16)
    @Conditional("fontFlagsHasLayout")
    public int fontDescent;

    @SWFType(BasicType.SI16)
    @Conditional("fontFlagsHasLayout")
    public int fontLeading;

    @SWFType(BasicType.SI16)
    @Conditional("fontFlagsHasLayout")
    public List<Integer> fontAdvanceTable;

    @Conditional("fontFlagsHasLayout")
    public List<RECT> fontBoundsTable;

    @Conditional("fontFlagsHasLayout")
    public List<KERNINGRECORD> fontKerningTable;

    public static final int ID = 48;

    @Override
    public boolean isSmall() {
        return fontFlagsSmallText;
    }

    @Override
    public int getGlyphWidth(int glyphIndex) {
        return glyphShapeTable.get(glyphIndex).getBounds().getWidth();
    }

    @Override
    public RECT getGlyphBounds(int glyphIndex) {
        if (fontFlagsHasLayout) {
            return fontBoundsTable.get(glyphIndex);
        }
        return super.getGlyphBounds(glyphIndex);
    }

    @Override
    public double getGlyphAdvance(int glyphIndex) {
        if (fontFlagsHasLayout) {
            return fontAdvanceTable.get(glyphIndex);
        } else {
            return -1;
        }
    }

    /**
     * Gets data bytes
     *
     * @return Bytes of data
     */
    @Override
    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SWFOutputStream sos = new SWFOutputStream(baos, getVersion());
        try {
            sos.writeUI16(fontId);
            sos.writeUB(1, fontFlagsHasLayout ? 1 : 0);
            sos.writeUB(1, fontFlagsShiftJIS ? 1 : 0);
            sos.writeUB(1, fontFlagsSmallText ? 1 : 0);
            sos.writeUB(1, fontFlagsANSI ? 1 : 0);
            sos.writeUB(1, fontFlagsWideOffsets ? 1 : 0);
            sos.writeUB(1, fontFlagsWideCodes ? 1 : 0);
            sos.writeUB(1, fontFlagsItalic ? 1 : 0);
            sos.writeUB(1, fontFlagsBold ? 1 : 0);
            sos.writeLANGCODE(languageCode);
            byte[] fontNameBytes = Utf8Helper.getBytes(fontName);
            sos.writeUI8(fontNameBytes.length);
            sos.write(fontNameBytes);
            int numGlyphs = glyphShapeTable.size();
            sos.writeUI16(numGlyphs);

            List<Long> offsetTable = new ArrayList<>();
            ByteArrayOutputStream baosGlyphShapes = new ByteArrayOutputStream();

            SWFOutputStream sos3 = new SWFOutputStream(baosGlyphShapes, getVersion());
            for (int i = 0; i < numGlyphs; i++) {
                offsetTable.add((glyphShapeTable.size() + 1/*CodeTableOffset*/) * (fontFlagsWideOffsets ? 4 : 2) + sos3.getPos());
                sos3.writeSHAPE(glyphShapeTable.get(i), 1);
            }
            byte[] baGlyphShapes = baosGlyphShapes.toByteArray();
            for (Long offset : offsetTable) {
                if (fontFlagsWideOffsets) {
                    sos.writeUI32(offset);
                } else {
                    sos.writeUI16((int) (long) offset);
                }
            }
            if (numGlyphs > 0) {
                long offset = (glyphShapeTable.size() + 1/*CodeTableOffset*/) * (fontFlagsWideOffsets ? 4 : 2) + baGlyphShapes.length;
                if (fontFlagsWideOffsets) {
                    sos.writeUI32(offset);
                } else {
                    sos.writeUI16((int) offset);
                }
                sos.write(baGlyphShapes);

                for (int i = 0; i < numGlyphs; i++) {
                    if (fontFlagsWideCodes) {
                        sos.writeUI16(codeTable.get(i));
                    } else {
                        sos.writeUI8(codeTable.get(i));
                    }
                }
            }
            if (fontFlagsHasLayout) {
                sos.writeSI16(fontAscent);
                sos.writeSI16(fontDescent);
                sos.writeSI16(fontLeading);
                for (int i = 0; i < numGlyphs; i++) {
                    sos.writeSI16(fontAdvanceTable.get(i));
                }
                for (int i = 0; i < numGlyphs; i++) {
                    sos.writeRECT(fontBoundsTable.get(i));
                }
                sos.writeUI16(fontKerningTable.size());
                for (int k = 0; k < fontKerningTable.size(); k++) {
                    sos.writeKERNINGRECORD(fontKerningTable.get(k), fontFlagsWideCodes);
                }
            }

        } catch (IOException e) {
            throw new Error("This should never happen.", e);
        }
        return baos.toByteArray();
    }

    /**
     * Constructor
     *
     * @param swf
     */
    public DefineFont2Tag(SWF swf) {
        super(swf, ID, "DefineFont2", null);
        fontId = swf.getNextCharacterId();
        languageCode = new LANGCODE();
        fontName = "New font";
        glyphShapeTable = new ArrayList<>();
        codeTable = new ArrayList<>();
    }

    /**
     * Constructor
     *
     * @param sis
     * @param data
     * @throws IOException
     */
    public DefineFont2Tag(SWFInputStream sis, ByteArrayRange data) throws IOException {
        super(sis.getSwf(), ID, "DefineFont2", data);
        readData(sis, data, 0, false, false, false);
    }

    @Override
    public final void readData(SWFInputStream sis, ByteArrayRange data, int level, boolean parallel, boolean skipUnusualTags, boolean lazy) throws IOException {
        fontId = sis.readUI16("fontId");
        fontFlagsHasLayout = sis.readUB(1, "fontFlagsHasLayout") == 1;
        fontFlagsShiftJIS = sis.readUB(1, "fontFlagsShiftJIS") == 1;
        fontFlagsSmallText = sis.readUB(1, "fontFlagsSmallText") == 1;
        fontFlagsANSI = sis.readUB(1, "fontFlagsANSI") == 1;
        fontFlagsWideOffsets = sis.readUB(1, "fontFlagsWideOffsets") == 1;
        fontFlagsWideCodes = sis.readUB(1, "fontFlagsWideCodes") == 1;
        fontFlagsItalic = sis.readUB(1, "fontFlagsItalic") == 1;
        fontFlagsBold = sis.readUB(1, "fontFlagsBold") == 1;
        languageCode = sis.readLANGCODE("languageCode");
        int fontNameLen = sis.readUI8("fontNameLen");
        if (swf.version >= 6) {
            fontName = new String(sis.readBytesEx(fontNameLen, "fontName"), Utf8Helper.charset);
        } else {
            fontName = new String(sis.readBytesEx(fontNameLen, "fontName"));
        }
        int numGlyphs = sis.readUI16("numGlyphs");
        long[] offsetTable = new long[numGlyphs];
        long pos = sis.getPos();
        for (int i = 0; i < numGlyphs; i++) { //offsetTable
            if (fontFlagsWideOffsets) {
                offsetTable[i] = sis.readUI32("offset");
            } else {
                offsetTable[i] = sis.readUI16("offset");
            }
        }
        if (numGlyphs > 0) { //codeTableOffset
            if (fontFlagsWideOffsets) {
                sis.readUI32("codeTableOffset");
            } else {
                sis.readUI16("codeTableOffset");
            }
        }

        glyphShapeTable = new ArrayList<>();
        for (int i = 0; i < numGlyphs; i++) {
            sis.seek(pos + offsetTable[i]);
            glyphShapeTable.add(sis.readSHAPE(1, false, "shape"));
        }

        codeTable = new ArrayList<>(); //[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
            if (fontFlagsWideCodes) {
                codeTable.add(sis.readUI16("code"));
            } else {
                codeTable.add(sis.readUI8("code"));
            }
        }

        if (fontFlagsHasLayout) {
            fontAscent = sis.readUI16("fontAscent");
            fontDescent = sis.readUI16("fontDescent");
            fontLeading = sis.readSI16("fontLeading");
            fontAdvanceTable = new ArrayList<>();
            for (int i = 0; i < numGlyphs; i++) {
                fontAdvanceTable.add(sis.readSI16("fontAdvance"));
            }
            fontBoundsTable = new ArrayList<>();
            for (int i = 0; i < numGlyphs; i++) {
                fontBoundsTable.add(sis.readRECT("rect"));
            }
            int kerningCount = sis.readUI16("kerningCount");
            fontKerningTable = new ArrayList<>();
            for (int i = 0; i < kerningCount; i++) {
                fontKerningTable.add(sis.readKERNINGRECORD(fontFlagsWideCodes, "record"));
            }
        }
    }

    @Override
    public int getFontId() {
        return fontId;
    }

    @Override
    public List<SHAPE> getGlyphShapeTable() {
        return glyphShapeTable;
    }

    @Override
    public int getCharacterId() {
        return fontId;
    }

    @Override
    public char glyphToChar(int glyphIndex) {
        return (char) (int) codeTable.get(glyphIndex);
    }

    @Override
    public int charToGlyph(char c) {
        return codeTable.indexOf((int) c);
    }

    @Override
    public String getFontNameIntag() {
        String ret = fontName;
        if (ret.contains("" + (char) 0)) {
            ret = ret.substring(0, ret.indexOf(0));
        }
        return ret;
    }

    @Override
    public boolean isBold() {
        return fontFlagsBold;
    }

    @Override
    public boolean isItalic() {
        return fontFlagsItalic;
    }

    @Override
    public boolean isSmallEditable() {
        return true;
    }

    @Override
    public boolean isBoldEditable() {
        return true;
    }

    @Override
    public boolean isItalicEditable() {
        return true;
    }

    @Override
    public void setSmall(boolean value) {
        fontFlagsSmallText = value;
    }

    @Override
    public void setBold(boolean value) {
        fontFlagsBold = value;
    }

    @Override
    public void setItalic(boolean value) {
        fontFlagsItalic = value;
    }

    @Override
    public double getDivider() {
        return 1;
    }

    @Override
    public int getAscent() {
        if (fontFlagsHasLayout) {
            return fontAscent;
        }
        return -1;
    }

    @Override
    public int getDescent() {
        if (fontFlagsHasLayout) {
            return fontDescent;
        }
        return -1;
    }

    @Override
    public int getLeading() {
        if (fontFlagsHasLayout) {
            return fontLeading;
        }
        return -1;
    }

    @Override
    public void addCharacter(char character, Font font) {
        int fontStyle = getFontStyle();

        SHAPE shp = SHAPERECORD.fontCharacterToSHAPE(font, (int) Math.round(getDivider() * 1024), character);

        int code = (int) character;
        int pos = -1;
        boolean exists = false;
        for (int i = 0; i < codeTable.size(); i++) {
            if (codeTable.get(i) >= code) {
                if (codeTable.get(i) == code) {
                    exists = true;
                }
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            pos = codeTable.size();
        }

        if (!exists) {
            shiftGlyphIndices(fontId, pos);
            glyphShapeTable.add(pos, shp);
            codeTable.add(pos, (int) character);
        } else {
            glyphShapeTable.set(pos, shp);
        }

        if (fontFlagsHasLayout) {
            Font fnt = new Font(fontName, fontStyle, 1024);
            if (!exists) {
                fontBoundsTable.add(pos, shp.getBounds());
                fontAdvanceTable.add(pos, (int) getDivider() * Math.round(FontHelper.getFontAdvance(fnt, character)));
            } else {
                fontBoundsTable.set(pos, shp.getBounds());
                fontAdvanceTable.set(pos, (int) getDivider() * Math.round(FontHelper.getFontAdvance(fnt, character)));
            }
        }

        setModified(true);
    }

    @Override
    public void setAdvanceValues(Font font) {
        boolean hasLayout = fontFlagsHasLayout;
        fontFlagsHasLayout = true;
        fontAdvanceTable = new ArrayList<>();
        if (!hasLayout) {
            fontBoundsTable = new ArrayList<>();
            fontKerningTable = new ArrayList<>();
        }

        for (Integer character : codeTable) {
            char ch = (char) (int) character;
            SHAPE shp = SHAPERECORD.fontCharacterToSHAPE(font, (int) Math.round(getDivider() * 1024), ch);
            fontBoundsTable.add(shp.getBounds());
            int fontStyle = getFontStyle();
            Font fnt = new Font(font.getFontName(), fontStyle, 1024); // Not multiplied with divider as it causes problems to create font with height around 20k
            fontAdvanceTable.add((int) getDivider() * Math.round(FontHelper.getFontAdvance(fnt, ch)));
        }
    }

    @Override
    public String getCharacters(List<Tag> tags) {
        StringBuilder ret = new StringBuilder();
        for (int i : codeTable) {
            ret.append((char) i);
        }
        return ret.toString();
    }

    @Override
    public boolean hasLayout() {
        return fontFlagsHasLayout;
    }

    @Override
    public int getGlyphKerningAdjustment(int glyphIndex, int nextGlyphIndex) {
        char c1 = glyphToChar(glyphIndex);
        char c2 = glyphToChar(nextGlyphIndex);
        return getCharKerningAdjustment(c1, c2);
    }

    @Override
    public int getCharKerningAdjustment(char c1, char c2) {
        int kerningAdjustment = 0;
        for (KERNINGRECORD ker : fontKerningTable) {
            if (ker.fontKerningCode1 == c1 && ker.fontKerningCode2 == c2) {
                kerningAdjustment = ker.fontKerningAdjustment;
                break;
            }
        }
        return kerningAdjustment;
    }
}
