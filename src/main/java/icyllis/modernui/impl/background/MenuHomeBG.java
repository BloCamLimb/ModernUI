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

package icyllis.modernui.impl.background;

import icyllis.modernui.animation.Animation;
import icyllis.modernui.animation.Applier;
import icyllis.modernui.animation.ITimeInterpolator;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.discard.IDrawable;

import javax.annotation.Nonnull;

public class MenuHomeBG implements IDrawable {

    private float height;

    private float xOffset = -32;

    public MenuHomeBG() {
        new Animation(200)
                .applyTo(
                        new Applier(xOffset, 0, () -> xOffset, v -> xOffset = v)
                                .setInterpolator(ITimeInterpolator.SINE))
                .start();
    }

    /*@Override
    public void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        RenderSystem.disableTexture();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(0, height, 0.0D).color(0, 0, 0, 180).endVertex();
        bufferbuilder.pos(32, height, 0.0D).color(0, 0, 0, 180).endVertex();
        bufferbuilder.pos(32, 0, 0.0D).color(0, 0, 0, 180).endVertex();
        bufferbuilder.pos(0, 0, 0.0D).color(0, 0, 0, 180).endVertex();
        tessellator.draw();
        bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0F);
        bufferbuilder.pos(32, 0, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferbuilder.pos(32, height, 0.0D).color(140, 140, 140, 220).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }*/

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.translate(xOffset, 0);
        //canvas.setColor(0, 0, 0, 0.7f);
        canvas.drawRect(0, 0, 32, height);
        //canvas.setColor(0.55f, 0.55f, 0.55f, 0.85f);
        canvas.drawLine(32, 0, 32, height);
    }

    public void resize(int width, int height) {
        this.height = height;
    }
}
