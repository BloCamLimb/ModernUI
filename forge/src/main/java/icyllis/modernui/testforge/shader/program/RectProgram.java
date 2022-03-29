/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.testforge.shader.program;

import icyllis.modernui.ModernUI;
import icyllis.modernui.opengl.GLProgram;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL43C;

import javax.annotation.Nonnull;

@Deprecated
public class RectProgram extends GLProgram {

    public static final ResourceLocation VERT = new ResourceLocation(ModernUI.ID, "shaders/pos_color.vert");
    public static final ResourceLocation VERT_TEX = new ResourceLocation(ModernUI.ID, "shaders/pos_color_tex.vert");

    private static RectProgram sFill;
    private static FillTex sFillTex;
    private static Feathered sFeathered;

    private RectProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        super();
    }

    public static void createPrograms() {
        if (sFill == null) {
            sFill = new RectProgram(VERT, new ResourceLocation(ModernUI.ID, "shaders/color_fill.frag"));
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
            super(VERT_TEX, new ResourceLocation(ModernUI.ID, "shaders/color_tex.frag"));
        }

        /*@Override
        public void link(ResourceManager manager) throws IOException {
            super.link(manager);
            GL43C.glProgramUniform1i(mId, 0, 0); // always use GL_TEXTURE0
        }*/
    }

    public static class Feathered extends RectProgram {

        private Feathered() {
            super(VERT, new ResourceLocation(ModernUI.ID, "shaders/rect_fill_v.frag"));
        }

        public void setThickness(float thickness) {
            GL43C.glUniform1f(0, thickness);
        }

        public void setInnerRect(float left, float top, float right, float bottom) {
            GL43C.glUniform4f(1, left, top, right, bottom);
        }
    }
}
