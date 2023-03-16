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

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.file.Path;

public class TestImageConversion {

    // convert to PNG
    public static void main(String[] args) {
        Configurator.setRootLevel(Level.INFO);
        String get = Bitmap.openDialogGet(null, null, null);
        if (get != null) {
            Path p = Path.of(get);
            var opts = new BitmapFactory.Options();
            opts.inDecodeMimeType = true;
            try (Bitmap bm = BitmapFactory.decodePath(p, opts)) {
                ModernUI.LOGGER.info("dimensions: {}x{}, format: {}, mimeType: {}",
                        opts.outWidth, opts.outHeight, opts.outFormat, opts.outMimeType);
                String name = p.getFileName().toString().replaceAll("\\..+", "");
                bm.saveDialog(Bitmap.SaveFormat.PNG, 0, name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
