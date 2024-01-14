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

package icyllis.modernui.test;

import icyllis.arc3d.engine.ISurface;
import icyllis.arc3d.opengl.GLTexture;
import icyllis.modernui.core.ActivityWindow;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL45C;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class TestLazyTextureProxyFromBitmap {

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.ALL);

        var get = Bitmap.openDialogGet(null, null, null);
        if (get == null) {
            return;
        }
        var opts = new BitmapFactory.Options();
        opts.inPreferredFormat = Bitmap.Format.RGBA_8888;
        var sourceBm = BitmapFactory.decodePath(Path.of(get), opts);

        Core.initialize();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        var window = ActivityWindow.createMainWindow("ProxyWindow", 16, 16);
        window.makeCurrent();
        if (!Core.initOpenGL()) {
            throw new RuntimeException("Failed to initialize OpenGL");
        }
        Core.glSetupDebugCallback();
        Core.glShowCapsErrorDialog();

        var dContext = Core.requireDirectContext();

        var proxy = dContext.getSurfaceProvider().createTextureFromPixels(
                sourceBm.getPixelMap(), sourceBm.getPixelRef(),
                sourceBm.getColorType(), ISurface.FLAG_BUDGETED
        );
        Objects.requireNonNull(proxy, "Failed to create proxy");
        proxy.instantiate(dContext.getResourceProvider());

        proxy.unref();

        proxy = dContext.getSurfaceProvider().createTextureFromPixels(
                sourceBm.getPixelMap(), sourceBm.getPixelRef(),
                sourceBm.getColorType(), ISurface.FLAG_BUDGETED
        );
        Objects.requireNonNull(proxy, "Failed to create proxy");
        proxy.instantiate(dContext.getResourceProvider());

        var outBm = Bitmap.createBitmap(sourceBm.getWidth(), sourceBm.getHeight(), Bitmap.Format.RGBA_8888);
        try {
            GL45C.glGetTextureImage(
                    ((GLTexture) (Objects.requireNonNull(proxy.getGpuTexture()))).getHandle(), 0,
                    GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE,
                    (int) outBm.getSize(), outBm.getAddress()
            );
            outBm.saveDialog(Bitmap.SaveFormat.PNG, 100, null);
        } finally {
            sourceBm.close();
            proxy.unref();
            outBm.close();
        }

        System.out.println(dContext.getDevice().getStats());

        dContext.unref();
        window.close();
        Core.terminate();
    }
}
