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

package icyllis.modernui.gui.scroll;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.option.ResourcePackGroup;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;

import java.util.function.Function;

public class ResourceScrollWindow extends ScrollWindow<ResourcePackGroup> {

    public ResourceScrollWindow(Function<Integer, Float> xAligner, Function<Integer, Float> wResizer) {
        super(xAligner, h -> 36f, wResizer, h -> h - 72f);
    }

    @Override
    public void drawEndExtra() {
        /*Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + height, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x + width, y + height, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x + width, y, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(0, 0, 0, 128).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();*/
    }
}
