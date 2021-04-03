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

package icyllis.modernui.core.mixin;

import com.mojang.blaze3d.platform.Window;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import icyllis.modernui.core.forge.MuiHooks;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.view.ViewConfig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {

    /**
     * @author BloCamLimb
     * @reason Make GUI scale more suitable, and not limited to even numbers when forceUnicode = true
     */
    @Overwrite
    public int calculateScale(int guiScaleIn, boolean forceUnicode) {
        int r = MuiHooks.C.calcGuiScales((Window) (Object) this);
        return guiScaleIn > 0 ? Mth.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    @Inject(method = "setGuiScale", at = @At("TAIL"))
    private void onSetGuiScale(double scaleFactor, CallbackInfo ci) {
        int i = (int) scaleFactor;
        if (i != scaleFactor) {
            ModernUI.LOGGER.warn(ModernUI.MARKER, "Gui scale should be an integer: {}", scaleFactor);
        }
        int oldLevel = Math.min((int) (ViewConfig.sViewScale + 0.5f), 3);
        int newLevel = Math.min((int) (i * 0.5f + 0.5f), 3);
        if (RenderCore.isEngineStarted() && oldLevel != newLevel) {
            TextLayoutProcessor.getInstance().reload();
        }
        // See standards
        GlyphManager.sResolutionLevel = newLevel;
        ViewConfig.sViewScale = i * 0.5f;
    }
}
