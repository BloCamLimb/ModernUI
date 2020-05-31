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

package icyllis.modernui.ui.layout;

import icyllis.modernui.ui.master.IViewRect;

public class ListLayout implements ILayout {

    @Override
    public int getLayoutX(IViewRect prev, IViewRect parent) {
        return prev.getLeft();
    }

    @Override
    public int getLayoutY(IViewRect prev, IViewRect parent) {
        return prev.getBottom();
    }

    @Override
    public int getLayoutWidth(IViewRect prev, IViewRect parent) {
        return prev.getWidth();
    }

    @Override
    public int getLayoutHeight(IViewRect prev, IViewRect parent) {
        return prev.getHeight();
    }
}
