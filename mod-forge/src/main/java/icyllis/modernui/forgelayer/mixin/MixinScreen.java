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

package icyllis.modernui.forgelayer.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(Screen.class)
public class MixinScreen {

    private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/block/dark_oak_planks.png");

    @Redirect(
            method = "renderBackground(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIII)V"
            )
    )
    private void renderBackgroundInWorld(@Nonnull Screen screen, @Nonnull MatrixStack stack, int x1, int y1, int x2, int y2,
                                         int colorA, int colorB) {
        BlurHandler.INSTANCE.drawScreenBackground(screen, stack, x1, y1, x2, y2);
    }

    @Redirect(
            method = "renderDirtBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"
            )
    )
    private void bindDirtBackgroundTexture(@Nonnull TextureManager textureManager, ResourceLocation rl) {
        textureManager.bindTexture(BACKGROUND);
    }
}
