/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.scroll;

import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.IWidget;

public interface IScrollHost extends IHost, IWidget {

    /**
     * Get scroll offset without top and bottom margin
     *
     * @return scroll amount (gt 0)
     */
    float getVisibleOffset();

    float getMargin();

    void layoutList();

    float getMaxScrollAmount();

    void callbackScrollAmount(float scrollAmount);

    ScrollController getScrollController();
}
