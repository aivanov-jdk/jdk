/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4308987 8377568
 * @summary Allow negative offset(s) in the constructors.
 */

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;

public class NegativeOffset {
    public static void main(String argv[]) {
        // This tests that the exception has been taken out of DataBuffer
        try {
            new DataBufferByte(new byte[10], 10, -1);
            new DataBufferShort(new short[10], 10, -1);
            new DataBufferUShort(new short[10], 10, -1);
            new DataBufferInt(new int[10], 10, -1);
            new DataBufferDouble(new double[10], 10, -1);
            new DataBufferFloat(new float[10], 10, -1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new RuntimeException("IllegalArgumentException should not be thrown for negative offset");
        }

    }
}
