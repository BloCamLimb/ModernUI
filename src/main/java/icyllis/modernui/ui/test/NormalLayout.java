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

package icyllis.modernui.ui.test;

@Deprecated
public class NormalLayout implements ILayout {

    private Align9D align = Align9D.CENTER;

    private int offsetX = 0;

    private int offsetY = 0;

    private int width = 16;

    private int height = 16;

    @Override
    public int getLayoutX(IViewRect prev, IViewRect parent) {
        return (int) (parent.getLeft() + align.getAlignedX(offsetX, parent.getWidth()));
    }

    @Override
    public int getLayoutY(IViewRect prev, IViewRect parent) {
        return (int) (parent.getTop() + align.getAlignedX(offsetY, parent.getHeight()));
    }

    @Override
    public int getLayoutWidth(IViewRect prev, IViewRect parent) {
        return width;
    }

    @Override
    public int getLayoutHeight(IViewRect prev, IViewRect parent) {
        return height;
    }
}
