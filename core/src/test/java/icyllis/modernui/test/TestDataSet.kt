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

package icyllis.modernui.test

import icyllis.modernui.util.DataSet
import icyllis.modernui.util.Parcel
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

fun main() {
    Configurator.setRootLevel(Level.ALL)
    val output = ByteArrayOutputStream();
    write(output)
    val bytes = output.toByteArray()
    val input = ByteArrayInputStream(bytes)
    read(input)
}

fun write(output: OutputStream) {
    val bundle = DataSet()
    bundle["health"] = 5
    bundle["velocity"] = 9.2f
    val pos = DataSet()
    pos["x"] = 6.1f
    pos["y"] = 56.2f
    bundle["pos"] = pos
    if ("pos" in bundle)
        println("Ok")
    Parcel.deflate(output, bundle)
}

fun read(input: InputStream) {
    val bundle = Parcel.inflate(input, null)
    println(bundle) // {health=5, velocity=9.2, pos={x=6.1, y=56.2}}
}
