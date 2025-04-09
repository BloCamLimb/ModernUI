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

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;

/**
 * A user interface element the user can tap or click to perform an action.
 *
 * <p>To specify an action when the button is pressed, set a click
 * listener on the button object in the corresponding fragment code:</p>
 *
 * <pre>{@code
 * public class MyFragment extends Fragment {
 *     @Nullable
 *     @Override
 *     public View onCreateView(@Nullable ViewGroup container, ...) {
 *         final Button button = new Button;
 *         button.setOnClickListener(v -> {
 *              // Code here executes on UI thread after user presses button
 *         });
 *         // ...
 *         return button;
 *     }
 * }
 * }</pre>
 *
 * <p>The above snippet creates an instance of {@link OnClickListener} and wires
 * the listener to the button using {@link #setOnClickListener}.
 * As a result, the system executes the code you write in lambda expression after the
 * user presses the button.</p>
 *
 * <p class="note">The system executes the code in {@code onClick} on the
 * <strong>UI thread</strong>. This means your onClick code must execute quickly to avoid
 * delaying your app's response to further user actions.
 */
public class Button extends TextView {

    @AttrRes
    static final ResourceId DEF_STYLE_ATTR =
            ResourceId.attr(R.ns, R.attr.buttonStyle);

    /**
     * Simple constructor to use when creating a button from code.
     * <p>
     * Note that this constructor will only initialize the text colors based on
     * the current theme, with no background or padding. You may call
     * {@link #Button(Context, AttributeSet)} with null attributes to use the default style.
     *
     * @param context The Context the Button is running in, through which it can
     *                access the current theme, resources, etc.
     * @see #Button(Context, AttributeSet)
     */
    public Button(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);
        setGravity(Gravity.CENTER);
    }

    /**
     * {@link LayoutInflater} calls this constructor when inflating a Button from XML.
     * The attributes defined by the current theme's
     * {@link R.attr#buttonStyle modernui:buttonStyle}
     * override base view attributes.
     * <p>
     * You may call this constructor to create your own button instance in code.
     * In addition, you must override this constructor when creating custom views.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML Button tag being used to inflate the view.
     * @see #Button(Context, AttributeSet, ResourceId)
     * @see icyllis.modernui.view.View#View(Context, AttributeSet)
     */
    public Button(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, DEF_STYLE_ATTR);
    }

    public Button(Context context, @Nullable AttributeSet attrs,
                  @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public Button(Context context, @Nullable AttributeSet attrs,
                  @Nullable @AttrRes ResourceId defStyleAttr,
                  @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
