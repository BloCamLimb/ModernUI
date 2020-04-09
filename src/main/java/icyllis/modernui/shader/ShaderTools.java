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

package icyllis.modernui.shader;

import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class ShaderTools {

    public enum Shaders {
        ;

        private ResourceLocation vert;

        private ResourceLocation frag;

        Shaders(String vert, String frag) {
            this.vert = new ResourceLocation(ModernUI.MODID, vert);
            this.frag = new ResourceLocation(ModernUI.MODID, frag);
        }
    }

    public static final FloatBuffer FLOAT_BUF = MemoryUtil.memAllocFloat(1);

    public static final Marker MARKER = MarkerManager.getMarker("SHADER");

    private static final Map<Shaders, ShaderProgram> PROGRAMS = new EnumMap<>(Shaders.class);

    static {
        Arrays.stream(Shaders.values()).forEach(shader -> createProgram(Minecraft.getInstance().getResourceManager(), shader));
    }

    public static void useShader(Shaders shader, Consumer<Integer> callback) {
        ShaderProgram instance = PROGRAMS.get(shader);

        if (instance != null) {
            int program = instance.getProgram();
            ShaderLinkHelper.func_227804_a_(program);

            if (callback != null) {
                callback.accept(program);
            }
        }
    }

    public static void releaseShader() {
        ShaderLinkHelper.func_227804_a_(0);
    }

    private static void createProgram(IResourceManager manager, Shaders shader) {
        try {
            ShaderLoader vertex = createShader(manager, shader.vert, ShaderLoader.ShaderType.VERTEX);
            ShaderLoader fragment = createShader(manager, shader.frag, ShaderLoader.ShaderType.FRAGMENT);
            int program = ShaderLinkHelper.createProgram();
            ShaderProgram shaderProgram = new ShaderProgram(program, vertex, fragment);
            ShaderLinkHelper.linkProgram(shaderProgram);
            PROGRAMS.put(shader, shaderProgram);
        } catch (IOException e) {
            ModernUI.LOGGER.fatal(MARKER, "Can't create program, please report this issue");
            e.printStackTrace();
        }
    }

    private static ShaderLoader createShader(IResourceManager manager, ResourceLocation location, ShaderLoader.ShaderType type) throws IOException {
        try (InputStream is = new BufferedInputStream(manager.getResource(location).getInputStream())) {
            return ShaderLoader.func_216534_a(type, location.toString(), is);
        }
    }
}
