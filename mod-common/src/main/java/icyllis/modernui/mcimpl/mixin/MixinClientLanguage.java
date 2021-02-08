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

package icyllis.modernui.mcimpl.mixin;

import icyllis.modernui.graphics.text.ModernFontRenderer;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ClientLanguage.class)
public abstract class MixinClientLanguage {

    @Shadow
    public abstract boolean isDefaultRightToLeft();

    /*@Shadow
    protected abstract String func_239500_d_(String p_239500_1_);


    @Overwrite
    public String func_230504_a_(String text, boolean token) {
        if (TrueTypeRenderer.sGlobalRenderer || !func_230505_b_()) {
            return text;
        } else {
            if (token && text.indexOf(37) != -1) {
                text = ClientLanguageMap.func_239499_c_(text);
            }
            return func_239500_d_(text);
        }
    }*/

    /**
     * Present = stopped, so return false
     *
     * @author BloCamLimb
     * @reason We have a text layout engine
     */
    @Overwrite
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        return ModernFontRenderer.isGlobalRenderer() ?
                copier -> !text.visit((s, t) ->
                        StringDecomposer.iterate(t, s, copier) ? Optional.empty()
                                : FormattedText.STOP_ITERATION, Style.EMPTY).isPresent()
                : FormattedBidiReorder.reorder(text, isDefaultRightToLeft());
    }
}
