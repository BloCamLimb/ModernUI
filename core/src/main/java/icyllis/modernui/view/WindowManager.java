/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.pipeline.DrawShadowUtils;
import org.jetbrains.annotations.ApiStatus;

/**
 * Interface for adding custom windows to the platform window.
 * <p>
 * Internally used by ToastManager and PopupWindow.
 */
@ApiStatus.Internal
public interface WindowManager extends ViewManager {

    class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * X position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#LEFT} or {@link Gravity#START} or {@link Gravity#RIGHT} or
         * {@link Gravity#END} it provides an offset from the given edge.
         */
        public int x;

        /**
         * Y position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#TOP} or {@link Gravity#BOTTOM} it provides
         * an offset from the given edge.
         */
        public int y;

        /**
         * Indicates how much of the extra space will be allocated horizontally
         * to the view associated with these LayoutParams. Specify 0 if the view
         * should not be stretched. Otherwise the extra pixels will be pro-rated
         * among all views whose weight is greater than 0.
         */
        public float horizontalWeight;

        /**
         * Indicates how much of the extra space will be allocated vertically
         * to the view associated with these LayoutParams. Specify 0 if the view
         * should not be stretched. Otherwise the extra pixels will be pro-rated
         * among all views whose weight is greater than 0.
         */
        public float verticalWeight;

        /**
         * Positive insets between the drawing surface and window content.
         *
         * @hidden
         */
        public final Rect surfaceInsets = new Rect();

        /**
         * Whether the surface insets have been manually set. When set to
         * {@code false}, the view root will automatically determine the
         * appropriate surface insets.
         *
         * @see #surfaceInsets
         * @hidden
         */
        public boolean hasManualSurfaceInsets;

        public int type;

        public static final int FIRST_APPLICATION_WINDOW = 1;

        public static final int TYPE_BASE_APPLICATION = 1;

        public static final int LAST_APPLICATION_WINDOW = 99;

        public static final int FIRST_SUB_WINDOW = 1000;

        public static final int TYPE_APPLICATION_PANEL = FIRST_SUB_WINDOW;

        public static final int TYPE_APPLICATION_SUB_PANEL = FIRST_SUB_WINDOW + 2;

        @ApiStatus.Internal
        public static final int TYPE_APPLICATION_ABOVE_SUB_PANEL = FIRST_SUB_WINDOW + 5;

        public static final int LAST_SUB_WINDOW = 1999;

        public static final int FIRST_SYSTEM_WINDOW = 2000;

        @ApiStatus.Internal
        public static final int TYPE_TOAST = FIRST_SYSTEM_WINDOW + 5;

        public static final int LAST_SYSTEM_WINDOW = 2999;

        public static final int FLAG_NOT_FOCUSABLE = 0x00000008;

        public static final int FLAG_NOT_TOUCH_MODAL = 0x00000020;

        public int flags;

        public int gravity;

        public float horizontalMargin;

        public float verticalMargin;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            type = TYPE_BASE_APPLICATION;
        }

        /**
         * True if the window should consume all pointer events itself, regardless of whether they
         * are inside of the window. If the window is modal, its touchable region will expand to the
         * size of its task.
         */
        @ApiStatus.Internal
        public boolean isModal() {
            return (flags & (FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE)) == 0;
        }

        /**
         * Sets the surface insets based on the elevation (visual z position) of the input view.
         * @hidden
         */
        public final void setSurfaceInsets(View view, boolean manual, boolean preservePrevious) {
            //TODO
            final int surfaceInset = (int) Math.ceil(view.getZ() * DrawShadowUtils.kOutsetPerZ);
            if (surfaceInset == 0) {
                // OK to have 0 (this is the case for non-freeform windows).
                surfaceInsets.set(0, 0, 0, 0);
            } else {
                surfaceInsets.set(
                        Math.max(surfaceInset, surfaceInsets.left),
                        Math.max(surfaceInset, surfaceInsets.top),
                        Math.max(surfaceInset, surfaceInsets.right),
                        Math.max(surfaceInset, surfaceInsets.bottom));
            }
            hasManualSurfaceInsets = manual;
        }
    }
}
