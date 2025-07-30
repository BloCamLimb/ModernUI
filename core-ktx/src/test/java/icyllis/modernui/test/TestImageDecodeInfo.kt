/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

fun main() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    val gets = Bitmap.openDialogGets(null, null, null)
    if (gets != null) {
        val opts = BitmapFactory.Options()
        opts.inDecodeMimeType = true
        gets.asSequence().map { s: String -> Path.of(s) }.forEach { p: Path ->
            try {
                BitmapFactory.decodePathInfo(p, opts)
                Log.LOGGER.info(
                    "file: {}\ndimensions: {}x{}, format: {}, mimeType: {}",
                    p, opts.outWidth, opts.outHeight, opts.outFormat, opts.outMimeType
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
