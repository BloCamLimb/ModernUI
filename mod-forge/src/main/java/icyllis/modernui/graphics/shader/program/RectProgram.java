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

package icyllis.modernui.graphics.shader.program;

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.texture.TextureUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL43;

import javax.annotation.Nonnull;
import java.io.IOException;

public class RectProgram extends ShaderProgram {

    public static final ResourceLocation VERT = new ResourceLocation(ModernUI.ID, "shaders/rect.vert");
    public static final ResourceLocation VERT_TEX = new ResourceLocation(ModernUI.ID, "shaders/rect_tex.vert");

    private static RectProgram sFill;
    private static FillTex sFillTex;
    private static Feathered sFeathered;

    private RectProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        super(vert, frag);
    }

    public static void createPrograms() {
        if (sFill == null) {
            sFill = new RectProgram(VERT, new ResourceLocation(ModernUI.ID, "shaders/rect_fill.frag"));
            sFillTex = new FillTex();
            sFeathered = new Feathered();
        }
    }

    public static RectProgram fill() {
        return sFill;
    }

    public static RectProgram fillTex() {
        return sFillTex;
    }

    public static Feathered feathered() {
        return sFeathered;
    }

    private static class FillTex extends RectProgram {

        private FillTex() {
            super(VERT_TEX, new ResourceLocation(ModernUI.ID, "shaders/rect_fill_tex.frag"));
        }

        @Override
        public void link(ResourceManager manager) throws IOException {
            super.link(manager);
            GL43.glProgramUniform1i(mId, 0, TextureUnit.TEXTURE0.getSampler()); // always use GL_TEXTURE0
        }
    }

    public static class Feathered extends RectProgram {

        private Feathered() {
            super(VERT, new ResourceLocation(ModernUI.ID, "shaders/rect_fill_v.frag"));
        }

        public void setThickness(float thickness) {
            GL43.glUniform1f(0, thickness);
        }

        public void setInnerRect(float left, float top, float right, float bottom) {
            GL43.glUniform4f(1, left, top, right, bottom);
        }
    }
}
