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

import icyllis.modernui.util.DataSet
import icyllis.modernui.util.Parcel

fun main() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    val parcel = Parcel()
    write(parcel)
    val pos = parcel.position()
    val cap = parcel.capacity()
    println("Bytes: $pos, Cap: $cap")
    parcel.position(0)
    read(parcel)
    parcel.freeData()
}

fun write(parcel: Parcel) {
    val bundle = DataSet()
    bundle["health"] = 5
    bundle["velocity"] = 9.2f
    val pos = DataSet()
    pos["x"] = 6.1f
    pos["y"] = 56.2f
    bundle["pos"] = pos
    bundle["extra"] = "MODERNUI MODERNUI MODERNUI MODERNUI MODERNUI"
    if ("pos" in bundle)
        println("Ok")
    parcel.writeDataSet(bundle)
}

fun read(parcel: Parcel) {
    val bundle = parcel.readDataSet(null)
    println(bundle) // {health=5, velocity=9.2, pos={x=6.1, y=56.2}}
}
