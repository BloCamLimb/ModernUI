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

package icyllis.modernui.view;

import icyllis.modernui.core.ContextWrapper;
import icyllis.modernui.widget.FrameLayout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Represents an application screen context, for handling lifecycle events.
 */
@OnlyIn(Dist.CLIENT)
public abstract class Screen extends ContextWrapper {

    UIManager window;

    public Screen() {
        super(null);
    }

    public abstract void onCreate();

    public void setContentView(View view, FrameLayout.LayoutParams params) {
        window.setContentView(view, params);
    }
}
