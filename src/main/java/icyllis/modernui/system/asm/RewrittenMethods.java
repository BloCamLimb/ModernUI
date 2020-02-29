/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system.asm;

import net.minecraft.util.math.MathHelper;

public class RewrittenMethods {

    /** MainWindow **/

    public static int calcGuiScale(int guiScaleIn, int framebufferWidth, int framebufferHeight) {
        // recheck
        int r = RewrittenMethods.calcGuiScales(framebufferWidth, framebufferHeight);
        return guiScaleIn > 0 ? MathHelper.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    public static int calcGuiScales(int framebufferWidth, int framebufferHeight) {

        double a1 = framebufferWidth / 16d;
        double a2 = framebufferHeight / 9d;
        double base = Math.min(a1, a2);
        double top = Math.max(a1, a2);

        int min;
        int max = MathHelper.clamp((int) (base / 32), 1, 0xf);
        if (max > 1) {
            int i = (int) (base / 64);
            int j = (int) (top / 64);
            min = MathHelper.clamp(j > i ? i + 1 : i, 2, 0xf);
        } else {
            min = 1;
        }

        int best;
        if (min > 1) {
            int i = (int) (base / 40);
            int j = (int) (top / 40);
            double v1 = base / (i * 40);
            if (v1 > 1.25 || j > i) {
                best = Math.min(max, i + 1);
            } else {
                best = i;
            }
        } else {
            best = 1;
        }

        return min << 8 | best << 4 | max;
    }
}
