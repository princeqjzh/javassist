/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>LineNumberTablec_attribute</code>.
 */
public class LineNumberAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"LineNumberTable"</code>.
     */
    public static final String tag = "LineNumberTable";

    LineNumberAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    private LineNumberAttribute(ConstPool cp, byte[] i) {
        super(cp, tag, i);
    }

    /**
     * Returns <code>line_number_table_length</code>.
     * This represents the number of entries in the table.
     */
    public int tableLength() {
        return ByteArray.readU16bit(info, 0);
    }

    /**
     * Returns <code>line_number_table[i].start_pc</code>.
     * This represents the index into the code array at which the code
     * for a new line in the original source file begins.
     *
     * @param i         the i-th entry.
     */
    public int startPc(int i) {
        return ByteArray.readU16bit(info, i * 4 + 2);
    }

    /**
     * Returns <code>line_number_table[i].line_number</code>.
     * This represents the corresponding line number in the original
     * source file.
     *
     * @param i         the i-th entry.
     */
    public int lineNumber(int i) {
        return ByteArray.readU16bit(info, i * 4 + 4);
    }

    /**
     * Returns the line number corresponding to the specified bytecode.
     *
     * @param pc        the index into the code array.
     */
    public int toLineNumber(int pc) {
        int n = tableLength();
        int i = 0;
        for (; i < n; ++i)
            if (pc < startPc(i))
                if (i == 0)
                    return lineNumber(0);
                else
                    break;

        return lineNumber(i - 1);
    }

    /**
     * Returns the index into the code array at which the code for
     * the specified line begins.
     *
     * @param line      the line number.
     * @return          -1 if the specified line is not found.
     */
    public int toStartPc(int line) {
        int n = tableLength();
        for (int i = 0; i < n; ++i)
            if (line == lineNumber(i))
                return startPc(i);

        return -1;
    }

    /**
     * Makes a copy.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        should be null.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        byte[] src = info;
        int num = src.length;
        byte[] dest = new byte[num];
        for (int i = 0; i < num; ++i)
            dest[i] = src[i];

        LineNumberAttribute attr = new LineNumberAttribute(newCp, dest);
        return attr;
    }
}