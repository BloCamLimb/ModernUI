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

package icyllis.modernui.test

import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.util.Log
import java.io.IOException
import java.nio.file.Path

// convert to PNG
fun main() {
    Log.setLevel(Log.DEBUG)
    println(Runtime.version())
    val get = Bitmap.openDialogGet(null, null, null)
    if (get != null) {
        val p = Path.of(get)
        val opts = BitmapFactory.Options()
        opts.inDecodeMimeType = true
        try {
            BitmapFactory.decodePath(p, opts).use { bm ->
                Log.info(
                    null,
                    "dimensions: {}x{}, format: {}, mimeType: {}",
                    opts.outWidth, opts.outHeight, opts.outFormat, opts.outMimeType
                )
                val name = p.fileName.toString().replace("\\..+".toRegex(), "")
                bm.saveDialog(Bitmap.SaveFormat.PNG, 100, name)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
