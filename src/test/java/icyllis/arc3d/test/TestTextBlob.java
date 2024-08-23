/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.Font;
import icyllis.arc3d.core.TextBlob;
import icyllis.arc3d.core.j2d.Typeface_JDK;

public class TestTextBlob {

    public static void main(String[] args) {

        Typeface_JDK typeface = new Typeface_JDK(
                new java.awt.Font("STXingKai", java.awt.Font.PLAIN, 1));

        Font font1 = new Font();
        font1.setTypeface(typeface);
        font1.setSize(40);
        font1.setEdging(Font.kAntiAlias_Edging);

        Font font2 = new Font();
        font2.setTypeface(typeface);
        font2.setSize(60);
        font2.setEdging(Font.kAlias_Edging);

        int[] glyphs = {5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67};
        float[] positions = {0, 0, 16, 0, 25, 0, 30, 0, 35, 0, 44, 2, 56, 0, 71.5f, 0, 80, 0,
                171, 0, 162, 0, 150, 0, 134.5f, 0, 120, 0, 105, 0, 92, 0, 88, 0};

        var builder = new TextBlob.Builder();

        {
            TextBlob blob1 = TextBlob.make(
                    glyphs, 0,
                    positions, 0,
                    glyphs.length,
                    font1, null
            );

            TextBlob blob2;
            {
                builder.allocRunPos(font1, glyphs.length, null)
                        .addGlyphs(glyphs, 0, glyphs.length)
                        .addPositions(positions, 0, glyphs.length);
                blob2 = builder.build();
            }

            System.out.println();
        }

        {
            int count = glyphs.length - 6 - 3;
            TextBlob blob1 = TextBlob.make(
                    glyphs, 6,
                    positions, 10,
                    count,
                    font2, null
            );

            TextBlob blob2;
            {
                builder.allocRunPos(font2, count, null)
                        .addGlyphs(glyphs, 6, count)
                        .addPositions(positions, 10, count);
                blob2 = builder.build();
            }

            System.out.println();
        }

        {
            int count1 = 10;
            int count2 = glyphs.length - count1;

            TextBlob blob;
            {
                builder.allocRunPos(font1, count1, null)
                        .addGlyphs(glyphs, 0, count1)
                        .addPositions(positions, 0, count1);
                builder.allocRunPos(font2, count2, null)
                        .addGlyphs(glyphs, count1, count2)
                        .addPositions(positions, count1 * 2, count2);
                blob = builder.build();
            }

            System.out.println();
        }
    }
}
