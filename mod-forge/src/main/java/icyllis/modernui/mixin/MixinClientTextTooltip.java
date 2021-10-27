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

package icyllis.modernui.mixin;

import com.mojang.math.Matrix4f;
import icyllis.modernui.mcgui.TooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(ClientTextTooltip.class)
public class MixinClientTextTooltip {

    @Redirect(method = "renderText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch" +
                    "(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;" +
                    "Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I"))
    private int drawText(@Nonnull Font font, FormattedCharSequence text, float x, float y,
                          int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource source,
                          boolean seeThrough, int colorBackground, int packedLight) {
        final int colorR = (Math.max((int) (TooltipRenderer.sAlpha * 255), 1) << 24) | 0xFFFFFF;
        return font.drawInBatch(text, x, y, colorR, dropShadow, matrix, source, seeThrough, colorBackground, packedLight);
    }
}
