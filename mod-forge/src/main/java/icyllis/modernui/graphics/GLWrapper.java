/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.platform.RenderCore;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nonnull;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.platform.RenderCore.MARKER;
import static org.lwjgl.opengl.GL43.*;

@RenderThread
public class GLWrapper {

    /**
     * The id that represents an null OpenGL object.
     */
    public static final int UNASSIGNED_ID = -1;

    public static final int DEFAULT_FRAMEBUFFER = 0;

    private static boolean sInitialized = false;

    /**
     * The value is determined when we have a OpenGL context.
     */
    private static int sMaxTextureSize = 1024;
    private static int sMaxRenderBufferSize = 2048;

    public static void initialize(@Nonnull GLCapabilities caps) {
        RenderCore.ensureRenderThread();
        if (sInitialized)
            return;

        sMaxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        sMaxRenderBufferSize = glGetInteger(GL_MAX_RENDERBUFFER_SIZE);

        int count = 0;
        if (!caps.GL_ARB_vertex_buffer_object) {
            LOGGER.fatal(MARKER, "ARB vertex buffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_explicit_attrib_location) {
            LOGGER.fatal(MARKER, "ARB explicit attrib location is not supported");
            count++;
        }
        if (!caps.GL_ARB_vertex_array_object) {
            LOGGER.fatal(MARKER, "ARB vertex array object is not supported");
            count++;
        }
        if (!caps.GL_ARB_framebuffer_object) {
            LOGGER.fatal(MARKER, "ARB framebuffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_uniform_buffer_object) {
            LOGGER.fatal(MARKER, "ARB uniform buffer object is not supported");
            count++;
        }
        if (!caps.GL_ARB_separate_shader_objects) {
            LOGGER.fatal(MARKER, "ARB separate shader objects is not supported");
            count++;
        }
        if (!caps.GL_ARB_explicit_uniform_location) {
            LOGGER.fatal(MARKER, "ARB explicit uniform location is not supported");
            count++;
        }

        int v;
        if ((v = GLWrapper.getMaxTextureSize()) < GlyphManager.TEXTURE_SIZE) {
            LOGGER.fatal(MARKER, "Max texture size is too small, {} available but requires {}", v, GlyphManager.TEXTURE_SIZE);
            count++;
        }

        if (!caps.OpenGL43) {
            String glVersion = GL43.glGetString(GL43.GL_VERSION);
            if (glVersion == null) glVersion = "UNKNOWN";
            else glVersion = glVersion.split(" ")[0];
            ModernUI.get().warnSetup("warning.modernui.old_opengl", "4.3", glVersion);
            LOGGER.fatal(MARKER, "OpenGL is too old, your version is {} but requires OpenGL 4.3", glVersion);
        }

        if (count > 0) {
            LOGGER.fatal(MARKER, "There are {} GL capabilities that are not supported by your graphics environment", count);
            LOGGER.fatal(MARKER, "Try to use dedicated GPU for Java applications or upgrade your graphics card driver");
        }

        sInitialized = true;
    }

    public static int getMaxTextureSize() {
        return sMaxTextureSize;
    }

    public static int getMaxRenderBufferSize() {
        return sMaxRenderBufferSize;
    }
}
