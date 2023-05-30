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

import icyllis.modernui.util.BinaryIO
import icyllis.modernui.util.DataSet
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

fun main(args: Array<String>) {
    Configurator.setRootLevel(Level.ALL)
    val output = ByteArrayOutputStream();
    write(output)
    val bytes = output.toByteArray()
    val input = ByteArrayInputStream(bytes)
    read(input)
}

fun write(output: OutputStream) {
    val data = DataSet()
    data["health"] = 5
    data["velocity"] = 9.2f
    val pos = DataSet()
    pos["x"] = 6.1f
    pos["y"] = 56.2f
    data["pos"] = pos
    if ("pos" in data)
        println("Ok")
    BinaryIO.deflate(output, data)
}

fun read(input: InputStream) {
    val data = BinaryIO.inflate(input, null)
    println(data) // {health=5, velocity=9.2, pos={x=6.1, y=56.2}}
}
