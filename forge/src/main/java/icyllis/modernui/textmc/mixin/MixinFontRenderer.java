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
import icyllis.modernui.textmc.FormattedTextWrapper;
import icyllis.modernui.textmc.ModernFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nonnull;
import java.util.Optional;

@Mixin(Font.class)
public abstract class MixinFontRenderer {

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int drawInBatch(@Nonnull String text, float x, float y, int color, boolean dropShadow,
                           @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                           int colorBackground, int packedLight, @Deprecated boolean bidiFlag) {
        return ModernFontRenderer.drawText(text, x, y, color, dropShadow, matrix, source, seeThrough,
                colorBackground, packedLight);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int drawInBatch(@Nonnull Component text, float x, float y, int color, boolean dropShadow,
                           @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                           int colorBackground, int packedLight) {
        return ModernFontRenderer.drawText(text, x, y, color, dropShadow, matrix, source, seeThrough,
                colorBackground, packedLight);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int drawInBatch(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                           @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                           int colorBackground, int packedLight) {
        if (text instanceof FormattedTextWrapper)
            // Handle Enchantment Table
            if (((FormattedTextWrapper) text).mText.visit((style, string) -> style.getFont().equals(Minecraft.ALT_FONT) ?
                    FormattedText.STOP_ITERATION : Optional.empty(), Style.EMPTY).isPresent())
                return callDrawInternal(text, x, y, color, dropShadow, matrix, source, seeThrough, colorBackground,
                        packedLight);
        return ModernFontRenderer.drawText(text, x, y, color, dropShadow, matrix, source, seeThrough,
                colorBackground, packedLight);
    }

    @Invoker
    abstract int callDrawInternal(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                                  @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                                  int colorBackground, int packedLight);

    /**
     * Bidi and shaping always works no matter what language is in.
     * So we should analyze the original string without reordering.
     * Do not reorder, we have our layout engine.
     *
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String bidirectionalShaping(String text) {
        return text;
    }
}
