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

import icyllis.modernui.system.MixinTools;
import net.minecraft.client.MainWindow;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MainWindow.class)
public class MixinMainWindow {

    /**
     * @author BloCamLimb
     * @reason Make GUI scale more suitable, and not limited to even numbers when forceUnicode = true
     */
    @Overwrite
    public int calcGuiScale(int guiScaleIn, boolean forceUnicode) {
        int r = MixinTools.calcGuiScales((MainWindow) (Object) this);
        return guiScaleIn > 0 ? MathHelper.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }
}
