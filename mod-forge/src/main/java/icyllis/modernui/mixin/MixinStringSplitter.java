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

import icyllis.modernui.textmc.ModernStringSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(StringSplitter.class)
public class MixinStringSplitter {

    @Shadow
    @Final
    private StringSplitter.WidthProvider widthProvider;

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nullable String text) {
        return ModernStringSplitter.measure(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nonnull FormattedText text) {
        return ModernStringSplitter.measure(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nonnull FormattedCharSequence text) {
        return ModernStringSplitter.measure(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int plainIndexAtWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.getTrimSize(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String plainHeadByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.trimText(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String plainTailByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.trimReverse(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    @Nullable
    public Style componentStyleAtWidth(@Nonnull FormattedText text, int width) {
        return ModernStringSplitter.styleAtWidth(text, width);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    @Nullable
    public Style componentStyleAtWidth(@Nonnull FormattedCharSequence text, int width) {
        return ModernStringSplitter.styleAtWidth(text, width);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public FormattedText headByWidth(@Nonnull FormattedText text, int width, @Nonnull Style style) {
        if (text instanceof TextComponent) {
            TextComponent c = (TextComponent) text;
            if (c.getStyle().getFont().equals(Minecraft.ALT_FONT)) {
                final float[] maxWidth = {width};
                final int[] position = {0};
                if (!StringDecomposer.iterate(c.getText(), c.getStyle(), (i, s, ch) -> {
                    maxWidth[0] -= widthProvider.getWidth(ch, s);
                    if (maxWidth[0] >= 0.0F) {
                        position[0] = i + Character.charCount(ch);
                        return true;
                    } else {
                        return false;
                    }
                })) {
                    String sub = c.getText().substring(0, position[0]);
                    if (!sub.isEmpty()) {
                        return FormattedText.of(sub, c.getStyle());
                    }
                } else {
                    if (!c.getText().isEmpty()) {
                        return FormattedText.of(c.getText(), c.getStyle());
                    }
                }
                return FormattedText.EMPTY;
            }
        }
        return ModernStringSplitter.trimText(text, width, style);
    }
}
