/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import icyllis.modernui.resources.ResourceId;

public final class R {

    /**
     * The namespace of {@literal modernui} resources.
     */
    public static final String ns = ModernUI.ID;

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
        /**
         * State value for StateListDrawable, set when a view or drawable is in the last position
         * in an ordered set. Actual usage may vary between views. Consult the host view
         * documentation for details.
         */
        public static final int state_last = 0x010100a6;
        /**
         * State value for StateListDrawable, set when the user is pressing down in a view.
         */
        public static final int state_pressed = 0x010100a7;
        /**
         * State identifier indicating the group is expanded.
         */
        public static final int state_expanded = 0x010100a8;
        /**
         * State identifier indicating the group is empty (has no children).
         */
        public static final int state_empty =0x010100a9;
        public static final int state_checkable = 0x0101009f;

        public static final String id = "id";
        public static final String tag = "tag";
        public static final String scrollX = "scrollX";
        public static final String scrollY = "scrollY";
        public static final String background = "background";
        public static final String padding = "padding";
        public static final String paddingLeft = "paddingLeft";
        public static final String paddingTop = "paddingTop";
        public static final String paddingRight = "paddingRight";
        public static final String paddingBottom = "paddingBottom";
        public static final String focusable = "focusable";
        public static final String focusableInTouchMode = "focusableInTouchMode";
        public static final String visibility = "visibility";
        public static final String clickable = "clickable";
        public static final String longClickable = "longClickable";
        public static final String contextClickable = "contextClickable";

        public static final String layout_width = "layout_width";
        public static final String layout_height = "layout_height";
        public static final String layout_margin = "layout_margin";
        public static final String layout_marginLeft = "layout_marginLeft";
        public static final String layout_marginTop = "layout_marginTop";
        public static final String layout_marginRight = "layout_marginRight";
        public static final String layout_marginBottom = "layout_marginBottom";

        public static final String checked = "checked";
        public static final String button = "button";

        public static final String min = "min";
        public static final String max = "max";
        public static final String progress = "progress";
        public static final String secondaryProgress = "secondaryProgress";
        public static final String indeterminate = "indeterminate";
        public static final String indeterminateDrawable = "indeterminateDrawable";
        public static final String progressDrawable = "progressDrawable";
        public static final String minWidth = "minWidth";
        public static final String minHeight = "minHeight";
        public static final String maxWidth = "maxWidth";
        public static final String maxHeight = "maxHeight";
        public static final String thumb = "thumb";
        public static final String track = "track";
        public static final String switchMinWidth = "switchMinWidth";
        public static final String switchPadding = "switchPadding";
        public static final String mirrorForRtl = "mirrorForRtl";

        public static final String textDirection = "textDirection";
        public static final String textAlignment = "textAlignment";
        public static final String layoutDirection = "layoutDirection";
        public static final String paddingStart = "paddingStart";
        public static final String paddingEnd = "paddingEnd";

        public static final String textAppearance = "textAppearance";
        public static final String textAppearanceInverse = "textAppearanceInverse";
        public static final String textColorPrimary = "textColorPrimary";
        public static final String textColorPrimaryDisableOnly = "textColorPrimaryDisableOnly";
        public static final String textColorSecondary = "textColorSecondary";
        public static final String textColorPrimaryInverse = "textColorPrimaryInverse";
        public static final String textColorSecondaryInverse = "textColorSecondaryInverse";
        public static final String textColorPrimaryNoDisable = "textColorPrimaryNoDisable";
        public static final String textColorSecondaryNoDisable = "textColorSecondaryNoDisable";
        public static final String textColorPrimaryInverseNoDisable = "textColorPrimaryInverseNoDisable";
        public static final String textColorSecondaryInverseNoDisable = "textColorSecondaryInverseNoDisable";
        public static final String textColorHintInverse = "textColorHintInverse";
        public static final String textAppearanceLarge = "textAppearanceLarge";
        public static final String textAppearanceMedium = "textAppearanceMedium";
        public static final String textAppearanceSmall = "textAppearanceSmall";
        public static final String textAppearanceLargeInverse = "textAppearanceLargeInverse";
        public static final String textAppearanceMediumInverse = "textAppearanceMediumInverse";
        public static final String textAppearanceSmallInverse = "textAppearanceSmallInverse";

        public static final String colorControlNormal = "colorControlNormal";
        public static final String colorControlActivated = "colorControlActivated";
        public static final String colorButtonNormal = "colorButtonNormal";
        public static final String colorControlHighlight = "colorControlHighlight";
        public static final String colorAccent = "colorAccent";
        public static final String colorEdgeEffect = "colorEdgeEffect";

        public static final String colorPrimary = "colorPrimary";
        public static final String colorOnPrimary = "colorOnPrimary";
        public static final String colorPrimaryInverse = "colorPrimaryInverse";
        public static final String colorPrimaryContainer = "colorPrimaryContainer";
        public static final String colorOnPrimaryContainer = "colorOnPrimaryContainer";
        public static final String colorPrimaryFixed = "colorPrimaryFixed";
        public static final String colorPrimaryFixedDim = "colorPrimaryFixedDim";
        public static final String colorOnPrimaryFixed = "colorOnPrimaryFixed";
        public static final String colorOnPrimaryFixedVariant = "colorOnPrimaryFixedVariant";
        public static final String colorSecondary = "colorSecondary";
        public static final String colorOnSecondary = "colorOnSecondary";
        public static final String colorSecondaryContainer = "colorSecondaryContainer";
        public static final String colorOnSecondaryContainer = "colorOnSecondaryContainer";
        public static final String colorSecondaryFixed = "colorSecondaryFixed";
        public static final String colorSecondaryFixedDim = "colorSecondaryFixedDim";
        public static final String colorOnSecondaryFixed = "colorOnSecondaryFixed";
        public static final String colorOnSecondaryFixedVariant = "colorOnSecondaryFixedVariant";
        public static final String colorTertiary = "colorTertiary";
        public static final String colorOnTertiary = "colorOnTertiary";
        public static final String colorTertiaryContainer = "colorTertiaryContainer";
        public static final String colorOnTertiaryContainer = "colorOnTertiaryContainer";
        public static final String colorTertiaryFixed = "colorTertiaryFixed";
        public static final String colorTertiaryFixedDim = "colorTertiaryFixedDim";
        public static final String colorOnTertiaryFixed = "colorOnTertiaryFixed";
        public static final String colorOnTertiaryFixedVariant = "colorOnTertiaryFixedVariant";
        public static final String colorBackground = "colorBackground";
        public static final String colorOnBackground = "colorOnBackground";
        public static final String colorSurface = "colorSurface";
        public static final String colorOnSurface = "colorOnSurface";
        public static final String colorSurfaceVariant = "colorSurfaceVariant";
        public static final String colorOnSurfaceVariant = "colorOnSurfaceVariant";
        public static final String colorSurfaceInverse = "colorSurfaceInverse";
        public static final String colorOnSurfaceInverse = "colorOnSurfaceInverse";
        public static final String colorSurfaceBright = "colorSurfaceBright";
        public static final String colorSurfaceDim = "colorSurfaceDim";
        public static final String colorSurfaceContainer = "colorSurfaceContainer";
        public static final String colorSurfaceContainerLow = "colorSurfaceContainerLow";
        public static final String colorSurfaceContainerHigh = "colorSurfaceContainerHigh";
        public static final String colorSurfaceContainerLowest = "colorSurfaceContainerLowest";
        public static final String colorSurfaceContainerHighest = "colorSurfaceContainerHighest";
        public static final String colorOutline = "colorOutline";
        public static final String colorOutlineVariant = "colorOutlineVariant";
        public static final String colorError = "colorError";
        public static final String colorOnError = "colorOnError";
        public static final String colorErrorContainer = "colorErrorContainer";
        public static final String colorOnErrorContainer = "colorOnErrorContainer";

        public static final String absListViewStyle = "absListViewStyle";
        public static final String autoCompleteTextViewStyle = "autoCompleteTextViewStyle";
        public static final String buttonStyle = "buttonStyle";
        public static final String checkboxStyle = "checkboxStyle";
        public static final String checkboxStyleMenuItem = "checkboxStyleMenuItem";
        public static final String dropDownListViewStyle = "dropDownListViewStyle";
        public static final String editTextStyle = "editTextStyle";
        public static final String expandableListViewStyle = "expandableListViewStyle";
        public static final String gridViewStyle = "gridViewStyle";
        public static final String imageButtonStyle = "imageButtonStyle";
        public static final String listViewStyle = "listViewStyle";
        public static final String popupWindowStyle = "popupWindowStyle";
        public static final String progressBarStyle = "progressBarStyle";
        public static final String progressBarStyleSmall = "progressBarStyleSmall";
        public static final String progressBarStyleHorizontal = "progressBarStyleHorizontal";
        public static final String progressBarStyleVertical = "progressBarStyleVertical";
        public static final String seekBarStyle = "seekBarStyle";
        public static final String radioButtonStyle = "radioButtonStyle";
        public static final String radioButtonStyleMenuItem = "radioButtonStyleMenuItem";
        public static final String scrollbarStyle = "scrollbarStyle";
        public static final String scrollViewStyle = "scrollViewStyle";
        public static final String spinnerStyle = "spinnerStyle";
        public static final String switchStyle = "switchStyle";
        public static final String textViewStyle = "textViewStyle";
        public static final String dropDownItemStyle = "dropDownItemStyle";
        public static final String spinnerDropDownItemStyle = "spinnerDropDownItemStyle";
        public static final String dropDownHintAppearance = "dropDownHintAppearance";
        public static final String spinnerItemStyle = "spinnerItemStyle";

        public static final String textSize = "textSize";
        public static final String textStyle = "textStyle";
        public static final String textColor = "textColor";
        public static final String textColorHighlight = "textColorHighlight";
        public static final String textColorHint = "textColorHint";
        public static final String textColorLink = "textColorLink";
        public static final String gravity = "gravity";

        public static final String textFontWeight = "textFontWeight";

        public static final String textAppearanceDisplayLarge = "textAppearanceDisplayLarge";
        public static final String textAppearanceDisplayMedium = "textAppearanceDisplayMedium";
        public static final String textAppearanceDisplaySmall = "textAppearanceDisplaySmall";
        public static final String textAppearanceHeadlineLarge = "textAppearanceHeadlineLarge";
        public static final String textAppearanceHeadlineMedium = "textAppearanceHeadlineMedium";
        public static final String textAppearanceHeadlineSmall = "textAppearanceHeadlineSmall";
        public static final String textAppearanceTitleLarge = "textAppearanceTitleLarge";
        public static final String textAppearanceTitleMedium = "textAppearanceTitleMedium";
        public static final String textAppearanceTitleSmall = "textAppearanceTitleSmall";
        public static final String textAppearanceBodyLarge = "textAppearanceBodyLarge";
        public static final String textAppearanceBodyMedium = "textAppearanceBodyMedium";
        public static final String textAppearanceBodySmall = "textAppearanceBodySmall";
        public static final String textAppearanceLabelLarge = "textAppearanceLabelLarge";
        public static final String textAppearanceLabelMedium = "textAppearanceLabelMedium";
        public static final String textAppearanceLabelSmall = "textAppearanceLabelSmall";

        /// INTERNAL BELOW \\\

        // Constant IDs for Material package.
        public static final int state_error = 0x05010001;
        public static final int state_indeterminate = 0x05010002;
        public static final int state_with_icon = 0x05010003;

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

        public static final int progress = 0x0102000d;
        public static final int secondaryProgress = 0x0102000f;

        public static final int mask = 0x0102002e;

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

    public static final class style {
        private static final String type = "style";

        public static final ResourceId Theme_Material3_Dark = new ResourceId(ns, type, "Theme.Material3.Dark");

        public static final ResourceId TextAppearance = new ResourceId(ns, type, "TextAppearance");
        public static final ResourceId TextAppearance_Material3_DisplayLarge = new ResourceId(ns, type, "TextAppearance.Material3.DisplayLarge");
        public static final ResourceId TextAppearance_Material3_DisplayMedium = new ResourceId(ns, type, "TextAppearance.Material3.DisplayMedium");
        public static final ResourceId TextAppearance_Material3_DisplaySmall = new ResourceId(ns, type, "TextAppearance.Material3.DisplaySmall");
        public static final ResourceId TextAppearance_Material3_HeadlineLarge = new ResourceId(ns, type, "TextAppearance.Material3.HeadlineLarge");
        public static final ResourceId TextAppearance_Material3_HeadlineMedium = new ResourceId(ns, type, "TextAppearance.Material3.HeadlineMedium");
        public static final ResourceId TextAppearance_Material3_HeadlineSmall = new ResourceId(ns, type, "TextAppearance.Material3.HeadlineSmall");
        public static final ResourceId TextAppearance_Material3_TitleLarge = new ResourceId(ns, type, "TextAppearance.Material3.TitleLarge");
        public static final ResourceId TextAppearance_Material3_TitleMedium = new ResourceId(ns, type, "TextAppearance.Material3.TitleMedium");
        public static final ResourceId TextAppearance_Material3_TitleSmall = new ResourceId(ns, type, "TextAppearance.Material3.TitleSmall");
        public static final ResourceId TextAppearance_Material3_BodyLarge = new ResourceId(ns, type, "TextAppearance.Material3.BodyLarge");
        public static final ResourceId TextAppearance_Material3_BodyMedium = new ResourceId(ns, type, "TextAppearance.Material3.BodyMedium");
        public static final ResourceId TextAppearance_Material3_BodySmall = new ResourceId(ns, type, "TextAppearance.Material3.BodySmall");
        public static final ResourceId TextAppearance_Material3_LabelLarge = new ResourceId(ns, type, "TextAppearance.Material3.LabelLarge");
        public static final ResourceId TextAppearance_Material3_LabelMedium = new ResourceId(ns, type, "TextAppearance.Material3.LabelMedium");
        public static final ResourceId TextAppearance_Material3_LabelSmall = new ResourceId(ns, type, "TextAppearance.Material3.LabelSmall");

        public static final ResourceId Widget_Material3_Button = new ResourceId(ns, type, "Widget.Material3.Button");
        public static final ResourceId Widget_Material3_Button_TonalButton = new ResourceId(ns, type, "Widget.Material3.Button.TonalButton");
        public static final ResourceId Widget_Material3_Button_OutlinedButton = new ResourceId(ns, type, "Widget.Material3.Button.OutlinedButton");
        public static final ResourceId Widget_Material3_Button_TextButton = new ResourceId(ns, type, "Widget.Material3.Button.TextButton");

        public static final ResourceId Widget_CompoundButton = new ResourceId(ns, type, "Widget.CompoundButton");
        public static final ResourceId Widget_Material3_CompoundButton_RadioButton = new ResourceId(ns, type, "Widget.Material3.CompoundButton.RadioButton");
        // unofficial
        public static final ResourceId Widget_Material3_CompoundButton_RadioButton_MenuItem = new ResourceId(ns, type, "Widget.Material3.CompoundButton.RadioButton.MenuItem");
        // unofficial
        public static final ResourceId Widget_Material3_CompoundButton_Switch = new ResourceId(ns, type, "Widget.Material3.CompoundButton.Switch");

        // unofficial
        public static final ResourceId Widget_Material3_ProgressBar = new ResourceId(ns, type, "Widget.Material3.ProgressBar");
        // unofficial
        public static final ResourceId Widget_Material3_ProgressBar_Small = new ResourceId(ns, type, "Widget.Material3.ProgressBar.Small");
        // unofficial
        public static final ResourceId Widget_Material3_ProgressBar_ExtraSmall = new ResourceId(ns, type, "Widget.Material3.ProgressBar.ExtraSmall");
        // unofficial
        public static final ResourceId Widget_Material3_ProgressBar_Horizontal = new ResourceId(ns, type, "Widget.Material3.ProgressBar.Horizontal");
        // unofficial
        public static final ResourceId Widget_Material3_ProgressBar_Vertical = new ResourceId(ns, type, "Widget.Material3.ProgressBar.Vertical");
    }

    static {
        __();
    }

    private static void __() {
        throw new UnsupportedOperationException();
    }
}
