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

import icyllis.arc3d.core.Rect2i
import icyllis.arc3d.core.RectanglePacker
import icyllis.modernui.graphics.MathUtil
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

const val WIDTH = 1024
const val HEIGHT = 1024

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.INFO)

    val packer1 = TestRectanglePacker(RectanglePacker.ALGORITHM_SKYLINE)
    val packer2 = TestRectanglePacker(RectanglePacker.ALGORITHM_HORIZON)
    val packer3 = TestRectanglePacker(RectanglePacker.ALGORITHM_HORIZON_OLD)
    val packer4 = TestRectanglePacker(RectanglePacker.ALGORITHM_BINARY_TREE)
    val packer5 = TestRectanglePacker(RectanglePacker.ALGORITHM_POWER2_LINE)

    val random = Random()

    GLFW.glfwInit()

    var hue = 0f
    do {
        val w = MathUtil.clamp(20 + (6 * random.nextGaussian()).toInt(), 8, 32)
        val h = MathUtil.clamp(24 + (6 * random.nextGaussian()).toInt(), 12, 36)
        val col = Color(icyllis.arc3d.core.Color.HSVToColor(hue, 1f, 1f))
        hue += 1f / 6f
    } while (
        packer1.add(w, h, col) or
        packer2.add(w, h, col) or
        packer3.add(w, h, col) or
        packer4.add(w, h, col) or
        packer5.add(w, h, col)
    )
    println("Algorithm Skyline:")
    packer1.print()

    println("-".repeat(20))

    println("Algorithm Horizon:")
    packer2.print()

    println("-".repeat(20))

    println("Algorithm Horizon (Old):")
    packer3.print()

    println("-".repeat(20))

    println("Algorithm Binary Tree:")
    packer4.print()

    println("-".repeat(20))

    println("Algorithm Power Of Two:")
    packer5.print()

    ImageIO.write(packer1.bm, "png", File("rect_packer1.png"))
    ImageIO.write(packer2.bm, "png", File("rect_packer2.png"))
    ImageIO.write(packer3.bm, "png", File("rect_packer3.png"))
    ImageIO.write(packer4.bm, "png", File("rect_packer4.png"))
    ImageIO.write(packer5.bm, "png", File("rect_packer5.png"))

    GLFW.glfwTerminate()
}

class TestRectanglePacker(algorithm: Int) {
    private val packer: RectanglePacker
    val bm: BufferedImage
    private val g2d: Graphics2D

    private var time = 0.0
    private var n = 0
    private var fails = 0

    init {
        packer = RectanglePacker.make(WIDTH, HEIGHT, algorithm)
        bm = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        g2d = bm.createGraphics()!!
    }

    fun add(w: Int, h: Int, color: Color): Boolean {
        if (fails >= 30) return false
        val rect = Rect2i(0, 0, w, h)
        val start = GLFW.glfwGetTime()
        val result = packer.addRect(rect)
        time += GLFW.glfwGetTime() - start
        if (result) {
            g2d.color = color
            g2d.fillRect(rect.x(), rect.y(), w, h)
            ++n
        } else {
            ++fails
        }
        return true
    }

    fun print() {
        println("$n rectangles")
        println("Coverage: " + packer.coverage + " (higher is better)")
        println("" + (time * 1.0E9 / n).toInt() + " nanoseconds per rectangle (lower is better)")
    }
}
