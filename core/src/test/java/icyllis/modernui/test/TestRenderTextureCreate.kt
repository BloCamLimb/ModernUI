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
import icyllis.arc3d.opengl.GLBackendFormat
import icyllis.arc3d.opengl.GLCore
import icyllis.arc3d.opengl.GLRenderTarget
import icyllis.modernui.core.Core
import icyllis.modernui.core.MainWindow
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.lwjgl.glfw.GLFW

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.ALL)

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

    val dContext = Core.requireDirectContext()

    val proxy = dContext.proxyProvider.createRenderTextureProxy(
        GLBackendFormat.make(GLCore.GL_RGBA8),
        1600, 900, 4,
        Surface.FLAG_BUDGETED + Surface.FLAG_MIPMAPPED
    )
    check(proxy != null) { "Failed to create proxy" }
    check(proxy.instantiate(dContext.resourceProvider))

    val rt = proxy.peekRenderTarget() as GLRenderTarget
    println(rt)
    println(rt.stencilBuffer)
    println(rt.sampleFramebuffer)
    println(rt.resolveFramebuffer)

    dContext.unref()
    window.close()
    Core.terminate()
}