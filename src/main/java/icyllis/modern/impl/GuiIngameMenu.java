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

package icyllis.modern.impl;

import icyllis.modern.api.animation.MotionType;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.ui.master.UniversalModernScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiIngameMenu extends UniversalModernScreen {

    public GuiIngameMenu(boolean isFullMenu) {
        super(l -> {
            Modules m = new Modules();
            l.add(m::createDefault);
        });
    }

    private static class Modules {

        void createDefault(IElementBuilder builder) {
            builder.colorRect()
                    .setColor(0x000000)
                    .setAbsPos(0, 0)
                    .setSize(Float::valueOf, Float::valueOf)
                    .applyToA(a -> a.setTarget(0.3f).setTiming(4));
            builder.colorRect()
                    .setColor(0x000000)
                    .setAlpha(0.5f)
                    .setAbsPos(0, 0)
                    .setSize(w -> 0f, Float::valueOf)
                    .applyToW(a -> a.setTarget(30).setTiming(3).setMotion(MotionType.SINE));
        }
    }
}
