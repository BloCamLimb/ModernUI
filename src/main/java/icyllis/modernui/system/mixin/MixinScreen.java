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

package icyllis.modernui.system.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.BlurHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(Screen.class)
public class MixinScreen {

    @Redirect(
            method = "renderBackground(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIII)V"
            )
    )
    private void renderBackgroundInWorld(@Nonnull Screen screen, @Nonnull MatrixStack stack, int x1, int y1, int x2, int y2,
                                         int colorA, int colorB) {
        int a = (int) (BlurHandler.INSTANCE.getBackgroundAlpha() * 255.0f);
        if (a == 0) {
            return;
        }
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Matrix4f matrix = stack.getLast().getMatrix();
        int z = screen.getBlitOffset();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        builder.pos(matrix, x2, y1, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x1, y1, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x1, y2, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x2, y2, z).color(0, 0, 0, a).endVertex();
        builder.finishDrawing();
        WorldVertexBufferUploader.draw(builder);

        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }
}
