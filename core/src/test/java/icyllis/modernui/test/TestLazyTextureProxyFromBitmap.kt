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

import icyllis.arc3d.engine.Surface
import icyllis.arc3d.opengl.GLCore
import icyllis.arc3d.opengl.GLTexture
import icyllis.modernui.core.Core
import icyllis.modernui.core.MainWindow
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.BitmapFactory
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL45C
import java.nio.file.Path

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.ALL)

    val get = Bitmap.openDialogGet(null, null, null) ?: return
    val opts = BitmapFactory.Options()
    opts.inPreferredFormat = Bitmap.Format.RGBA_8888
    val sourceBm = BitmapFactory.decodePath(Path.of(get), opts)

    Core.initialize()

    GLFW.glfwDefaultWindowHints()
    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API)
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

    val window = MainWindow.initialize("ProxyWindow", 16, 16)
    window.makeCurrent()
    check(Core.initOpenGL()) { "Failed to initialize OpenGL" }
    GLCore.setupDebugCallback()
    GLCore.showCapsErrorDialog()

    val dContext = Core.requireDirectContext()

    var proxy = dContext.proxyProvider.createProxyFromBitmap(sourceBm, sourceBm.colorType, Surface.FLAG_BUDGETED)
    check(proxy != null) { "Failed to create proxy" }
    proxy.instantiate(dContext.resourceProvider)

    proxy.unref()

    proxy = dContext.proxyProvider.createProxyFromBitmap(sourceBm, sourceBm.colorType, Surface.FLAG_BUDGETED)
    check(proxy != null) { "Failed to create proxy" }
    proxy.instantiate(dContext.resourceProvider)

    val outBm = Bitmap.createBitmap(sourceBm.width, sourceBm.height, Bitmap.Format.RGBA_8888)
    try {
        GL45C.glGetTextureImage(
            (proxy.peekTexture() as GLTexture).handle, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE,
            outBm.size, outBm.pixels
        )
        outBm.saveDialog(Bitmap.SaveFormat.PNG, 100, null)
    } finally {
        sourceBm.close()
        proxy.unref()
        outBm.close()
    }

    println(dContext.engine.stats)

    dContext.unref()
    window.close()
    Core.terminate()
}
