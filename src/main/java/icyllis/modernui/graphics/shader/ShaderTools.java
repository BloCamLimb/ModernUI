/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.shader;

import icyllis.modernui.graphics.shader.uniform.UniformFloat;
import icyllis.modernui.graphics.shader.uniform.UniformMatrix4f;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderLoader;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

public class ShaderTools {

    public static final Marker MARKER = MarkerManager.getMarker("SHADER");

    public static ShaderProgram ALPHA;

    public static void addResourceListener() {
        IResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).addReloadListener(
                    (ISelectiveResourceReloadListener) ShaderTools::compileShaders);
            ModernUI.LOGGER.debug("Shader listener added");
        }
    }

    private static void compileShaders(IResourceManager manager, Predicate<IResourceType> typePredicate) {
        /*if (ALPHA != null) {
            deleteShader(ALPHA);
        }
        ALPHA = createProgram(manager,
                new ResourceLocation(ModernUI.MODID, "shader/pass_through.vert"),
                new ResourceLocation(ModernUI.MODID, "shader/alpha.frag"));
        if (COLOR_SPRITE_SHADER != null) {
            deleteShader(COLOR_SPRITE_SHADER);
        }
        COLOR_SPRITE_SHADER = createProgram(manager,
                new ResourceLocation(ModernUI.MODID, "shader/sprite.vert"),
                new ResourceLocation(ModernUI.MODID, "shader/sprite.frag"));
        ModernUI.LOGGER.debug(MARKER, "Shaders have been compiled");*/
    }

    public static void useShader(@Nonnull ShaderProgram shader) {
        int program = shader.getProgram();
        ShaderLinkHelper.func_227804_a_(program);
    }

    public static void releaseShader() {
        ShaderLinkHelper.func_227804_a_(0);
    }

    public static void deleteShader(@Nonnull ShaderProgram shader) {
        ShaderLinkHelper.deleteShader(shader);
    }

    public static UniformFloat getUniformFloat(ShaderProgram shader, String name) {
        return new UniformFloat(GL20.glGetUniformLocation(shader.getProgram(), name));
    }

    public static UniformMatrix4f getUniformMatrix4f(ShaderProgram shader, String name) {
        int loc = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (loc == -1) {
            throw new RuntimeException();
        }
        return new UniformMatrix4f(loc);
    }

    /*@SuppressWarnings("unchecked")
    @Nonnull
    private static <T extends ShaderProgram> T createProgram(IResourceManager manager, ResourceLocation vsh, ResourceLocation fsh) {
        try {
            ShaderLoader vertex = createShader(manager, vsh, ShaderLoader.ShaderType.VERTEX);
            ShaderLoader fragment = createShader(manager, fsh, ShaderLoader.ShaderType.FRAGMENT);
            int program = ShaderLinkHelper.createProgram();
            ShaderProgram shaderProgram = new ShaderProgram(program, vertex, fragment);
            ShaderLinkHelper.linkProgram(shaderProgram);
            return (T) shaderProgram;
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(MARKER, "Can't create program, please report this issue");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }*/

    @Nonnull
    public static ShaderLoader createShader(IResourceManager manager, @Nonnull ResourceLocation location, ShaderLoader.ShaderType type) throws IOException {
        try (InputStream is = new BufferedInputStream(manager.getResource(location).getInputStream())) {
            return ShaderLoader.func_216534_a(type, location.toString(), is);
        }
    }
}
