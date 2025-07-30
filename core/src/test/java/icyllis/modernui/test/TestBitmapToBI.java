/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.util.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestBitmapToBI {

    // Decode an image using ModernUI Bitmap API
    // Copy a subset to BufferedImage and convert to INT_ARGB format
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        String get = Bitmap.openDialogGet(null, null, null);
        if (get == null) {
            return;
        }
        Bitmap bitmap;
        try (var fc = FileChannel.open(Path.of(get), StandardOpenOption.READ)) {
            bitmap = BitmapFactory.decodeChannel(fc);
        } catch (IOException e) {
            Log.LOGGER.error("", e);
            return;
        }
        Log.LOGGER.info(bitmap.toString());
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int subW = w / 2, subH = h / 2;
        if (subW < 2 || subH < 2) {
            bitmap.close();
            return;
        }
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        try (bitmap) {
            int[] rgb = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            bitmap.getPixels(rgb, (subH / 2) * w + subW / 2, w, subW / 2, subH / 2, subW, subH);
        }
        try {
            ImageIO.write(bi, "png", new File("bitmap_to_argb_to_imageio.png"));
        } catch (IOException e) {
            Log.LOGGER.error("", e);
        }
    }
}
