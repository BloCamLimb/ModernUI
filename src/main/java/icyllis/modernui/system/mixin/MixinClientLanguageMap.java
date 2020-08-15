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

import icyllis.modernui.font.TrueTypeRenderer;
import net.minecraft.client.resources.ClientLanguageMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientLanguageMap.class)
public abstract class MixinClientLanguageMap {

    /*@Shadow
    public abstract boolean func_230505_b_();

    @Shadow
    protected abstract String func_239500_d_(String p_239500_1_);

    *//**
     * @author BloCamLimb
     * @reason Fix vanilla's bug
     *//*
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
}
