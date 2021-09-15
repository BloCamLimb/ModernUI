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
import icyllis.modernui.textmc.FormattedTextWrapper;
import icyllis.modernui.textmc.ModernFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
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
        if (text instanceof FormattedTextWrapper) {
            if (((FormattedTextWrapper) text).mText.visit((s, t) -> s.getFont().equals(Minecraft.ALT_FONT) ?
                    FormattedText.STOP_ITERATION : Optional.empty(), Style.EMPTY).isPresent()) {
                return callDrawInternal(text, x, y, color, dropShadow, matrix, source, seeThrough, colorBackground,
                        packedLight);
            }
        }/* else if (!text.accept((i, s, ch) -> !s.getFont().equals(Minecraft.ALT_FONT))) {
            return callDrawInternal(text, x, y, color, dropShadow, matrix, source, seeThrough, colorBackground,
                    packedLight);
        }*/
        return ModernFontRenderer.drawText(text, x, y, color, dropShadow, matrix, source, seeThrough,
                colorBackground, packedLight);
    }

    @Invoker
    abstract int callDrawInternal(@Nonnull FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                                  @Nonnull Matrix4f matrix, @Nonnull MultiBufferSource source, boolean seeThrough,
                                  int colorBackground, int packedLight);

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine, do not reorder, we have our layout engine
     */
    @Overwrite
    public String bidirectionalShaping(String text) {
        return text;
    }
}
