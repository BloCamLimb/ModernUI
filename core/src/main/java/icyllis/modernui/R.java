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
         * State identifier indicating that the object is currently checked.
         */
        public static final int state_checked = 0x010100a0;
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

        public static final int background = 0x01020000;
        public static final int checkbox = 0x01020001;
        public static final int content = 0x01020002;
        public static final int edit = 0x01020003;
        public static final int empty = 0x01020004;
        public static final int hint = 0x01020005;

        public static final int input = 0x01020009;

        public static final int title = 0x01020016;
        public static final int toggle = 0x01020017;

        public static final int button1 = 0x01020019;
        public static final int button2 = 0x0102001a;
        public static final int button3 = 0x0102001b;

        /**
         * Context menu ID for the "Select All" menu item to select all text in a text view.
         */
        public static final int selectAll = 0x0102001f;

        /**
         * Context menu ID for the "Cut" menu item to copy and delete the currently selected
         * (or all) text in a text view to the clipboard.
         */
        public static final int cut = 0x01020020;

        /**
         * Context menu ID for the "Copy" menu item to copy the currently selected (or all)
         * text in a text view to the clipboard.
         */
        public static final int copy = 0x01020021;

        /**
         * Context menu ID for the "Paste" menu item to copy the current contents of the
         * clipboard into the text view.
         */
        public static final int paste = 0x01020022;

        public static final int undo = 0x01020032;
        public static final int redo = 0x01020033;

        /// INTERNAL BELOW \\\

        // Constant IDs for Fragment package.
        public static final int fragment_container_view_tag = 0x02020001;
        public static final int visible_removing_fragment_view_tag = 0x02020002;
        public static final int special_effects_controller_view_tag = 0x02020003;

        // Constant IDs for Lifecycle package.
        public static final int view_tree_lifecycle_owner = 0x03020001;
        public static final int view_tree_view_model_store_owner = 0x03020002;

        // Constant IDs for Transition package.
        public static final int transition_current_scene = 0x04020001;
        public static final int save_overlay_view = 0x04020002;
        public static final int transition_position = 0x04020003;

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
