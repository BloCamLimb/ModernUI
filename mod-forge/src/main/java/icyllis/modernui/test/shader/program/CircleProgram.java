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
import icyllis.modernui.graphics.shader.GLProgram;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL43C;

import javax.annotation.Nonnull;

@Deprecated
public class CircleProgram extends GLProgram {

    private static Fill sFill;
    private static Stroke sStroke;

    private CircleProgram(@Nonnull ResourceLocation vert, @Nonnull ResourceLocation frag) {
        super();
    }

    public static void createPrograms() {
        if (sFill == null) {
            sFill = new Fill();
            sStroke = new Stroke();
        }
    }

    public static Fill fill() {
        return sFill;
    }

    public static Stroke stroke() {
        return sStroke;
    }

    public void setCenter(float x, float y) {
        GL43C.glUniform2f(1, x, y);
    }

    public static class Fill extends CircleProgram {

        private Fill() {
            super(RectProgram.VERT, new ResourceLocation(ModernUI.ID, "shaders/circle_fill.frag"));
        }

        public void setRadius(float radius, float feather) {
            // feather <= radius
            GL43C.glUniform2f(0, radius, feather);
        }
    }

    public static class Stroke extends CircleProgram {

        private Stroke() {
            super(RectProgram.VERT, new ResourceLocation(ModernUI.ID, "shaders/circle_stroke.frag"));
        }

        public void setRadius(float inner, float radius, float feather) {
            // feather <= (radius - inner) / 2
            GL43C.glUniform3f(0, inner, radius, feather);
        }
    }
}
