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

package icyllis.modernui.textmc.mixin;

import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(SignRenderer.class)
public class MixinSignRenderer {

    @Shadow
    @Final
    private static int BLACK_TEXT_OUTLINE_COLOR;

    @Redirect(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;" +
            "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch8xOutline" +
                    "(Lnet/minecraft/util/FormattedCharSequence;FFIILcom/mojang/math/Matrix4f;" +
                    "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void onDrawGlowingText(Font font, @Nonnull FormattedCharSequence text, float x, float y,
                                   int color, int outlineColor, @Nonnull Matrix4f matrix,
                                   @Nonnull MultiBufferSource source, int packedLight) {
        if (outlineColor != BLACK_TEXT_OUTLINE_COLOR) {
            // use brighter color
            outlineColor = color;
        }
        font.drawInBatch8xOutline(text, x, y, color, outlineColor, matrix, source, packedLight);
    }
}
