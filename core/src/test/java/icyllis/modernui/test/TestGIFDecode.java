/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.image.GIFDecoder;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.util.Log;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestGIFDecode {

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        String get = Bitmap.openDialogGet(null, null, null);
        if (get != null) {
            Path p = Path.of(get);
            byte[] data;
            ByteBuffer buf, pixels = null;
            try (var fc = FileChannel.open(p, StandardOpenOption.READ)) {
                data = new byte[(int) fc.size()];
                buf = ByteBuffer.wrap(data);
                fc.read(buf);
                buf.flip();
                GIFDecoder dec = new GIFDecoder(buf);
                Log.LOGGER.info("FileSize {}, ScreenDim {}x{}", buf.limit(), dec.getScreenWidth(),
                        dec.getScreenHeight());
                pixels = MemoryUtil.memAlloc(dec.getScreenWidth() * dec.getScreenHeight() * 4);
                for (int i = 0; i < 500; i++) {
                    pixels.position(0);
                    int delay = dec.decodeNextFrame(pixels);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MemoryUtil.memFree(pixels);
            }
        }
    }
}
