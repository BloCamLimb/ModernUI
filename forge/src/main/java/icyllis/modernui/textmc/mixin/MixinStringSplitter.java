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

import icyllis.modernui.textmc.ModernStringSplitter;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

@Mixin(StringSplitter.class)
public class MixinStringSplitter {

    /*@Shadow
    @Final
    private StringSplitter.WidthProvider widthProvider;*/

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nullable String text) {
        return ModernStringSplitter.measureText(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nonnull FormattedText text) {
        return ModernStringSplitter.measureText(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public float stringWidth(@Nonnull FormattedCharSequence text) {
        return ModernStringSplitter.measureText(text);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int plainIndexAtWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.indexByWidth(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String plainHeadByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.headByWidth(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String plainTailByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.tailByWidth(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public int formattedIndexByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.indexByWidth(text, width, style);
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
    public String formattedHeadByWidth(@Nonnull String text, int width, @Nonnull Style style) {
        return ModernStringSplitter.headByWidth(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public FormattedText headByWidth(@Nonnull FormattedText text, int width, @Nonnull Style style) {
        // Handle Enchantment Table
        /*if (text instanceof Component component &&
                component.getSiblings().isEmpty() &&
                component.getStyle().getFont().equals(Minecraft.ALT_FONT) &&
                component.getContents() instanceof LiteralContents literal) {
            final MutableFloat maxWidth = new MutableFloat(width);
            final MutableInt position = new MutableInt();
            if (!StringDecomposer.iterate(literal.text(), component.getStyle(),
                    (index, sty, codePoint) -> {
                        if (maxWidth.addAndGet(-widthProvider.getWidth(codePoint, sty)) >= 0) {
                            position.setValue(index + Character.charCount(codePoint));
                            return true;
                        } else {
                            return false;
                        }
                    })) {
                String substring = literal.text().substring(0, position.intValue());
                if (!substring.isEmpty()) {
                    return FormattedText.of(substring, component.getStyle());
                }
            } else {
                if (!literal.text().isEmpty()) {
                    return FormattedText.of(literal.text(), component.getStyle());
                }
            }
            return FormattedText.EMPTY;
        }*/
        return ModernStringSplitter.headByWidth(text, width, style);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public void splitLines(@Nonnull String text, int width, @Nonnull Style style, boolean withEndSpace,
                           @Nonnull StringSplitter.LinePosConsumer linePos) {
        ModernStringSplitter.computeLineBreaks(text, width, style, linePos);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public void splitLines(@Nonnull FormattedText text, int width, @Nonnull Style style,
                           @Nonnull BiConsumer<FormattedText, Boolean> consumer) {
        ModernStringSplitter.computeLineBreaks(text, width, style, consumer);
    }
}
