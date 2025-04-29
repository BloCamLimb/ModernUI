/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Outline;
import icyllis.modernui.graphics.drawable.Drawable;

/**
 * Interface by which a View builds its {@link Outline}, used for shadow casting and clipping.
 */
@FunctionalInterface
public interface ViewOutlineProvider {
    /**
     * Default outline provider for Views, which queries the Outline from the View's background,
     * or generates a 0 alpha, rectangular Outline the size of the View if a background
     * isn't present.
     *
     * @see Drawable#getOutline(Outline)
     */
    ViewOutlineProvider BACKGROUND = (view, outline) -> {
        Drawable background = view.getBackground();
        if (background != null) {
            background.getOutline(outline);
        } else {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
            outline.setAlpha(0.0f);
        }
    };

    /**
     * Maintains the outline of the View to match its rectangular bounds,
     * at <code>1.0f</code> alpha.
     * <p>
     * This can be used to enable Views that are opaque but lacking a background cast a shadow.
     */
    ViewOutlineProvider BOUNDS = (view, outline) -> outline.setRect(0, 0, view.getWidth(), view.getHeight());

    /**
     * Maintains the outline of the View to match its rectangular padded bounds,
     * at <code>1.0f</code> alpha.
     * <p>
     * This can be used to enable Views that are opaque but lacking a background cast a shadow.
     */
    ViewOutlineProvider PADDED_BOUNDS = (view, outline) -> outline.setRect(
            view.getPaddingLeft(),
            view.getPaddingTop(),
            view.getWidth() - view.getPaddingRight(),
            view.getHeight() - view.getPaddingBottom());

    /**
     * Called to get the provider to populate the Outline.
     * <p>
     * This method will be called by a View when its owned Drawables are invalidated, when the
     * View's size changes, or if {@link View#invalidateOutline()} is called
     * explicitly.
     * <p>
     * The input outline is empty and has an alpha of <code>1.0f</code>.
     *
     * @param view    The view building the outline.
     * @param outline The empty outline to be populated.
     */
    void getOutline(@NonNull View view, @NonNull Outline outline);
}
