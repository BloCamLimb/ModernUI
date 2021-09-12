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
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mixin(StringSplitter.class)
public class MixinStringSplitter {

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
        return ModernStringSplitter.trimText(text, width, style);
    }
}
