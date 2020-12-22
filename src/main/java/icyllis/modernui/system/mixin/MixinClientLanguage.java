/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.system.mixin;

import icyllis.modernui.font.MuiFontRenderer;
import net.minecraft.client.resources.ClientLanguageMap;
import net.minecraft.client.util.BidiReorderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextProcessing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ClientLanguageMap.class)
public abstract class MixinClientLanguage {

    @Shadow
    public abstract boolean func_230505_b_();

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
     * @reason Do not reorder, Mojang
     */
    @Overwrite
    public IReorderingProcessor func_241870_a(ITextProperties text) {
        return MuiFontRenderer.isGlobalRenderer() ?
                copier -> !text.getComponentWithStyle((s, t) ->
                        TextProcessing.func_238341_a_(t, s, copier) ? Optional.empty()
                                : AccessTextProcessing.stopIteration(), Style.EMPTY).isPresent()
                : BidiReorderer.func_243508_a(text, func_230505_b_());
    }
}
