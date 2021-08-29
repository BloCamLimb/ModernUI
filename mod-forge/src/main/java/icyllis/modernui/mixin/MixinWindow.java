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

import com.mojang.blaze3d.platform.Window;
import icyllis.modernui.ModernUI;
import icyllis.modernui.forge.MForgeCompat;
import icyllis.modernui.graphics.font.GlyphManagerForge;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.textmc.TextLayoutProcessor;
import icyllis.modernui.view.ViewConfig;
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
        int r = MForgeCompat.calcGuiScales((Window) (Object) this);
        return guiScaleIn > 0 ? MathUtil.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    @Inject(method = "setGuiScale", at = @At("TAIL"))
    private void onSetGuiScale(double scaleFactor, CallbackInfo ci) {
        int i = (int) scaleFactor;
        if (i != scaleFactor) {
            ModernUI.LOGGER.warn(ModernUI.MARKER, "Gui scale should be an integer: {}", scaleFactor);
        }
        int oldLevel = Math.min((int) (ViewConfig.get().getViewScale() + 0.5f), 3);
        int newLevel = Math.min((int) (i * 0.5f + 0.5f), 3);
        if (RenderCore.isInitialized() && oldLevel != newLevel) {
            TextLayoutProcessor.getInstance().reload();
        }
        // See standards
        GlyphManagerForge.sResolutionLevel = newLevel;
        ViewConfig.get().setViewScale(i * 0.5f);
    }
}
