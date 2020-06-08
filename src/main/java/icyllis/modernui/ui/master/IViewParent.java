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

package icyllis.modernui.ui.master;

/**
 * Defines a view that can act as a parent of another view
 */
public interface IViewParent {

    /**
     * Get parent view
     *
     * @return parent view
     */
    IViewParent getParent();

    /**
     * Get relative mouse X if needed
     */
    double getRelativeMX();

    /**
     * Get relative mouse Y if needed
     */
    double getRelativeMY();

    /**
     * Transform relative X if needed
     *
     * @return absolute x
     */
    float toAbsoluteX(float rx);

    /**
     * Transform relative Y if needed
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

    /**
     * Required when child views changed, to request this parent to relayout all child views
     */
    void relayoutChildViews();
}
