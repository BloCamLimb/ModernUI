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

import icyllis.modernui.graphics.Rect
import icyllis.modernui.graphics.RectanglePacker
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

const val WIDTH = 1024
const val HEIGHT = 1024

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.INFO)

    val bm1 = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g2d1 = bm1.createGraphics()!!

    val bm2 = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g2d2 = bm2.createGraphics()!!

    val bm3 = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g2d3 = bm3.createGraphics()!!

    val bm4 = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g2d4 = bm4.createGraphics()!!

    val bm5 = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val g2d5 = bm5.createGraphics()!!

    val packer1 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_SKYLINE)
    val packer2 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_HORIZON)
    val packer3 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_HORIZON_OLD)
    val packer4 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_BINARY_TREE)
    val packer5 = RectanglePacker.make(WIDTH, HEIGHT, RectanglePacker.ALGORITHM_POWER2_LINE)
    val random = Random()

    var time1 = 0.0
    var time2 = 0.0
    var time3 = 0.0
    var time4 = 0.0
    var time5 = 0.0

    GLFW.glfwInit()

    var n1 = 0
    var n2 = 0
    var n3 = 0
    var n4 = 0
    var n5 = 0
    var fails = 0
    while (fails < 3000) {
        val w = random.nextInt(8, 33)
        val h = random.nextInt(12, 37)
        val col = Color(random.nextInt(0x1000000))

        val rect = Rect(0, 0, w, h)
        var start = GLFW.glfwGetTime()
        if (packer1.addRect(rect)) {
            g2d1.color = col
            g2d1.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1)
            ++n1
        } else {
            ++fails
        }
        time1 += GLFW.glfwGetTime() - start

        rect.set(0, 0, w, h)
        start = GLFW.glfwGetTime()
        if (packer2.addRect(rect)) {
            g2d2.color = col
            g2d2.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1)
            ++n2
        } else {
            ++fails
        }
        time2 += GLFW.glfwGetTime() - start

        rect.set(0, 0, w, h)
        start = GLFW.glfwGetTime()
        if (packer3.addRect(rect)) {
            g2d3.color = col
            g2d3.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1)
            ++n3
        } else {
            ++fails
        }
        time3 += GLFW.glfwGetTime() - start

        rect.set(0, 0, w, h)
        start = GLFW.glfwGetTime()
        if (packer4.addRect(rect)) {
            g2d4.color = col
            g2d4.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1)
            ++n4
        } else {
            ++fails
        }
        time4 += GLFW.glfwGetTime() - start

        rect.set(0, 0, w, h)
        start = GLFW.glfwGetTime()
        if (packer5.addRect(rect)) {
            g2d5.color = col
            g2d5.drawRect(rect.left, rect.top, rect.width() - 1, rect.height() - 1)
            ++n5
        } else {
            ++fails
        }
        time5 += GLFW.glfwGetTime() - start
    }
    println("Algorithm Skyline:")
    println("$n1 rectangles")
    println("Coverage: " + packer1.coverage + " (↑)")
    System.out.printf("Took %d microseconds (↓)\n", (time1 * 1000000).toInt())

    println("-".repeat(20))

    println("Algorithm Horizontal Line:")
    println("$n2 rectangles")
    println("Coverage: " + packer2.coverage + " (↑)")
    System.out.printf("Took %d microseconds (↓)\n", (time2 * 1000000).toInt())

    println("-".repeat(20))

    println("Algorithm Horizontal Line Memoryless:")
    println("$n3 rectangles")
    println("Coverage: " + packer3.coverage + " (↑)")
    System.out.printf("Took %d microseconds (↓)\n", (time3 * 1000000).toInt())

    println("-".repeat(20))

    println("Algorithm Binary Tree:")
    println("$n4 rectangles")
    println("Coverage: " + packer4.coverage + " (↑)")
    System.out.printf("Took %d microseconds (↓)\n", (time4 * 1000000).toInt())

    println("-".repeat(20))

    println("Algorithm Power 2 Line:")
    println("$n5 rectangles")
    println("Coverage: " + packer5.coverage + " (↑)")
    System.out.printf("Took %d microseconds (↓)\n", (time5 * 1000000).toInt())

    ImageIO.write(bm1, "png", File("rect_packer1.png"))
    ImageIO.write(bm2, "png", File("rect_packer2.png"))
    ImageIO.write(bm3, "png", File("rect_packer3.png"))
    ImageIO.write(bm4, "png", File("rect_packer4.png"))
    ImageIO.write(bm5, "png", File("rect_packer5.png"))

    GLFW.glfwTerminate()
}
