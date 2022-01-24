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

import icyllis.modernui.textmc.FormattedTextWrapper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mixin(Language.class)
public class MixinLanguage {

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @OnlyIn(Dist.CLIENT)
    @Overwrite
    public List<FormattedCharSequence> getVisualOrder(@Nonnull List<FormattedText> texts) {
        List<FormattedCharSequence> list = new ArrayList<>(texts.size());
        for (FormattedText text : texts) {
            list.add(new FormattedTextWrapper(text));
        }
        return list;
    }
}
