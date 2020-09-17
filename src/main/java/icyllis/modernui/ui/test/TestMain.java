/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestMain {

    public static void main(String[] args) throws IOException {

        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 32);
        graphics2D.setFont(font);
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, 1024, 1024);
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        String s = "\u0e01\u0e25\u0e31\u0e1a\u0e40\u0e02\u0e49\u0e32\u0e2a\u0e39\u0e48\u0e40\u0e01\u0e21";
        s = "\u0e23\u0e32\u0e22\u0e07\u0e32\u0e19\u0e1a\u0e31\u0e4a\u0e01";
        //s = "\u090f\u0915\u0932\u0916\u093f\u0932\u093e\u0921\u093c\u0940";
        GlyphVector vector = font.layoutGlyphVector(graphics2D.getFontRenderContext(), s.toCharArray(),
                0, s.length(), Font.LAYOUT_LEFT_TO_RIGHT);
        graphics2D.drawGlyphVector(vector, 20, 50);
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isCombining());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isLigature());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isStandard());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isComponent());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).isWhitespace());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).getAdvanceX());
        }
        System.out.println();
        for (int i = 0; i < s.length(); i++) {
            System.out.println(vector.getGlyphMetrics(i).getBounds2D());
        }


        ImageIO.write(image, "png", new File("F:/a.png"));
    }
}
