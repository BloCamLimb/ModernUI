/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui;

public final class R {

    public static final class attr {

        /**
         * State identifier indicating the popup will be above the anchor.
         */
        public static final int state_above_anchor = 0x010100aa;
        /**
         * Set when a view's window has input focus.
         */
        public static final int state_window_focused = 0x0101009d;
        /**
         * Set when a view (or one of its parents) is currently selected.
         */
        public static final int state_selected = 0x010100a1;
        /**
         * Set when a view has input focus.
         */
        public static final int state_focused = 0x01010562;
        /**
         * Set when a view is enabled.
         */
        public static final int state_enabled = 0x0101009e;
        /**
         * Set when the user is pressing down in a view.
         */
        public static final int state_pressed = 0x010100a7;
        /**
         * Set when a view or its parent has been "activated" meaning the user has currently
         * marked it as being of interest.
         */
        public static final int state_activated = 0x010102fe;
        /**
         * Set when a pointer is hovering over a view.
         */
        public static final int state_hovered = 0x01010367;
        /**
         * Set when a view that is capable of accepting a drop of the content currently
         * being manipulated in a drag-and-drop operation.
         */
        public static final int state_drag_can_accept = 0x01010368;
        /**
         * Set when a view is currently positioned over by a drag operation.
         */
        public static final int state_drag_hovered = 0x01010369;

        static {
            __();
        }
    }

    public static final class id {

        public static final int content = 0x01020002;

        static {
            __();
        }
    }

    static {
        __();
    }

    private static void __() {
        throw new UnsupportedOperationException();
    }
}
