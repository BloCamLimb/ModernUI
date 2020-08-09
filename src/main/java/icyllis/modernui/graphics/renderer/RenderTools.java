/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.renderer;

import icyllis.modernui.font.glyph.GlyphManager;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.program.*;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class RenderTools {

    public static final Marker MARKER = MarkerManager.getMarker("Render");

    public static int glCapabilitiesErrors;

    public static void init() {
        ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(
                (ISelectiveResourceReloadListener) RenderTools::onResourcesReload);
    }

    private static void onResourcesReload(IResourceManager manager, @Nonnull Predicate<IResourceType> t) {
        if (!t.test(VanillaResourceType.SHADERS)) {
            return;
        }
        boolean successful;
        int count = 0;
        try {
            RingShader.INSTANCE.compile(manager);
            count++;
            RoundedRectShader.INSTANCE.compile(manager);
            count++;
            RoundedFrameShader.INSTANCE.compile(manager);
            count++;
            CircleShader.INSTANCE.compile(manager);
            count++;
            FeatheredRectShader.INSTANCE.compile(manager);
            count++;
            successful = true;
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(MARKER, "An error occurred while compiling shaders", e);
            successful = false;
        }

        if (successful) {
            ModernUI.LOGGER.debug(MARKER, "There are {} shaders compiled successfully", count);
        }
    }

    public static <T extends ShaderProgram> void useShader(@Nonnull T shader) {
        int program = shader.getProgram();
        ShaderLinkHelper.func_227804_a_(program);
    }

    public static void releaseShader() {
        ShaderLinkHelper.func_227804_a_(0);
    }

    /**
     * Check GL capabilities and log incompatibilities
     *
     * @since 2.0.5
     */
    static void checkCapabilities() {
        GLCapabilities capabilities = GL.getCapabilities();
        int i = 0;
        if (!capabilities.GL_ARB_vertex_buffer_object) {
            ModernUI.LOGGER.fatal(MARKER, "Vertex buffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_explicit_attrib_location) {
            ModernUI.LOGGER.fatal(MARKER, "Explicit attrib location is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_vertex_array_object) {
            ModernUI.LOGGER.fatal(MARKER, "Vertex array object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_framebuffer_object) {
            ModernUI.LOGGER.fatal(MARKER, "Framebuffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_uniform_buffer_object) {
            ModernUI.LOGGER.fatal(MARKER, "Uniform buffer object is not supported");
            i++;
        }
        if (!capabilities.GL_ARB_explicit_uniform_location) {
            ModernUI.LOGGER.fatal(MARKER, "Explicit uniform location is not supported");
            i++;
        }

        int v;
        if ((v = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)) < GlyphManager.TEXTURE_SIZE) {
            ModernUI.LOGGER.fatal(MARKER, "Max texture size is too small, supplies {} but requires {}", v, GlyphManager.TEXTURE_SIZE);
            i++;
        }

        if (i == 0) {
            ModernUI.LOGGER.debug(MARKER, "All GL capabilities are successfully passed");
        } else {
            ModernUI.LOGGER.fatal(MARKER, "There are {} GL capabilities that are not supported, render system is not working properly", i);
            glCapabilitiesErrors = i;
        }
    }

    /*@Nonnull
    public static UniformFloat getUniformFloat(@Nonnull ShaderProgram shader, String name) {
        return new UniformFloat(GL20.glGetUniformLocation(shader.getProgram(), name));
    }

    @Nonnull
    public static UniformMatrix4f getUniformMatrix4f(@Nonnull ShaderProgram shader, String name) {
        int loc = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (loc == -1) {
            throw new RuntimeException();
        }
        return new UniformMatrix4f(loc);
    }*/
}
