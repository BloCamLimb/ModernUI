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

import com.mojang.brigadier.ParseResults;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(CommandSuggestions.class)
public abstract class MixinCommandSuggestions {

    @Shadow
    @Nullable
    private ParseResults<SharedSuggestionProvider> currentParse;

    @Invoker
    private static FormattedCharSequence callFormatText(ParseResults<SharedSuggestionProvider> parse,
                                                        String viewText, int baseOffset) {
        throw new IllegalStateException();
    }

    /**
     * @author BloCamLimb
     * @reason Optimization
     */
    @Nullable
    @Overwrite
    private FormattedCharSequence formatChat(String viewText, int baseOffset) {
        if (currentParse != null) {
            return callFormatText(currentParse, viewText, baseOffset);
        }
        // fast path
        return null;
    }
}
