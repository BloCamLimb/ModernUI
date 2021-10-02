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

package icyllis.modernui.test.shader.program;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.shader.GLProgram;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL43C;

import javax.annotation.Nonnull;

@Deprecated
public class RoundRectProgram extends GLProgram {

    private static Fill sFill;
    private static FillTex sFillTex;
    private static Stroke sStroke;

    private RoundRectProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        super();
    }

    @RenderThread
    public static void createPrograms() {
        if (sFill == null) {
            sFill = new Fill();
            sFillTex = new FillTex();
            sStroke = new Stroke();
        }
    }

    public static Fill fill() {
        return sFill;
    }

    public static FillTex fillTex() {
        return sFillTex;
    }

    public static Stroke stroke() {
        return sStroke;
    }

    public void setInnerRect(float left, float top, float right, float bottom) {
        GL43C.glUniform4f(1, left, top, right, bottom);
    }

    public static class Fill extends RoundRectProgram {

        private Fill() {
            super(RectProgram.VERT, new ResourceLocation(ModernUI.ID, "shaders/round_rect_fill.frag"));
        }

        private Fill(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
            super(vert, frag);
        }

        public void setRadius(float radius, float feather) {
            // 0 <= feather <= radius
            GL43C.glUniform2f(0, radius, feather);
        }
    }

    public static class FillTex extends Fill {

        private FillTex() {
            super(RectProgram.VERT_TEX, new ResourceLocation(ModernUI.ID, "shaders/round_rect_tex.frag"));
        }

        /*@Override
        public void link(ResourceManager manager) throws IOException {
            super.link(manager);
            GL43C.glProgramUniform1i(mId, 2, 0); // always use GL_TEXTURE0
        }*/
    }

    public static class Stroke extends RoundRectProgram {

        private Stroke() {
            super(RectProgram.VERT, new ResourceLocation(ModernUI.ID, "shaders/round_rect_stroke.frag"));
        }

        public void setRadius(float radius, float feather, float thickness) {
            // 0 <= feather <= thickness <= radius
            GL43C.glUniform3f(0, radius, feather, thickness);
        }
    }
}
