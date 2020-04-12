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

package icyllis.modernui.graphics.sprite.shader;

import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.nio.FloatBuffer;

public class ColorSpriteShader extends SpriteShader {

    public static ColorSpriteShader INSTANCE;

    public static FloatBuffer colorBuf;
    public static FloatBuffer posBuf;

    static {
        IResourceManager manager = Minecraft.getInstance().getResourceManager();
        try {
            ShaderLoader vertex = ShaderTools.createShader(manager, new ResourceLocation(ModernUI.MODID, "shader/sprite.vert"), ShaderLoader.ShaderType.VERTEX);
            ShaderLoader fragment = ShaderTools.createShader(manager, new ResourceLocation(ModernUI.MODID, "shader/sprite.frag"), ShaderLoader.ShaderType.FRAGMENT);
            int program = ShaderLinkHelper.createProgram();
            INSTANCE = new ColorSpriteShader(program, vertex, fragment);
            ShaderLinkHelper.linkProgram(INSTANCE);
            INSTANCE.findPointers();
        } catch (IOException e) {
            e.printStackTrace();
        }
        colorBuf = BufferUtils.createFloatBuffer(16);
        colorBuf.position(0);
        colorBuf.put(1).put(1).put(1).put(1);
        colorBuf.put(1).put(1).put(1).put(1);
        colorBuf.put(1).put(1).put(1).put(1);
        colorBuf.put(1).put(1).put(1).put(1);
        colorBuf.position(0);
        posBuf = BufferUtils.createFloatBuffer(12);
        posBuf.position(0);
        posBuf.put(20).put(40).put(0);
        posBuf.put(100).put(40).put(0);
        posBuf.put(100).put(20).put(0);
        posBuf.put(20).put(20).put(0);
        posBuf.position(0);
    }

    public ColorSpriteShader(int program, ShaderLoader vertex, ShaderLoader fragment) {
        super(program, vertex, fragment);
    }
}
