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

package icyllis.modernui.ui.master;

import javax.annotation.Nonnull;

/**
 * A view that can act as a parent
 */
public interface IViewParent extends IViewRect {

    /**
     * Get parent view, null if this view is UIManager
     *
     * @return parent view
     */
    IViewParent getParent();

    /**
     * Get relative mouse x if needed
     */
    double getRelativeMX();

    /**
     * Get relative mouse x if needed
     */
    double getRelativeMY();

    /**
     * Transform relative x if needed
     *
     * @return absolute x
     */
    float toAbsoluteX(float rx);

    /**
     * Transform relative y if needed
     *
     * @return absolute y
     */
    float toAbsoluteY(float ry);

    /**
     * Available in scrollable view
     */
    float getTranslationX();

    /**
     * Available in scrollable view
     */
    float getTranslationY();

    void relayoutChild(@Nonnull View view);
}
