/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.app.Activity;
import icyllis.modernui.core.Context;
import org.intellij.lang.annotations.MagicConstant;

/**
 * A toast is a view containing a quick little message for the user.  The toast class
 * helps you create and show those.
 *
 * <p>
 * When the view is shown to the user, appears as a floating view over the
 * application.  It will never receive focus.  The user will probably be in the
 * middle of typing something else.  The idea is to be as unobtrusive as
 * possible, while still showing the user the information you want them to see.
 * Example is the brief message saying that your settings have been saved.
 * <p>
 * The easiest way to use this class is to call the static method that constructs
 * everything you need and returns a new Toast object.
 * <p>
 * Note that toasts being sent from the background are rate limited, so avoid sending
 * such toasts in quick succession.
 */
public final class Toast {

    /**
     * Show the view or text notification for a short period of time.  This time
     * could be user-definable.  This is the default.
     *
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 0;

    /**
     * Show the view or text notification for a long period of time.  This time
     * could be user-definable.
     *
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 1;

    private final Context mContext;

    private int mDuration;

    /**
     * Text to be shown, in case this is NOT a custom toast (e.g. created with {@link
     * #makeText(Context, CharSequence, int)} or its variants).
     */
    private CharSequence mText;

    /**
     * Use {@link #makeText(Context, CharSequence, int)} instead.
     */
    private Toast(@NonNull Context context) {
        mContext = context;
    }

    @Deprecated(forRemoval = true)
    public static Toast makeText(@NonNull CharSequence text, int duration) {
        return makeText(ModernUI.getInstance(), text, duration);
    }

    /**
     * Make a standard toast that just contains text.
     *
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    @NonNull
    public static Toast makeText(@NonNull Context context, @NonNull CharSequence text,
                                 @MagicConstant(intValues = {LENGTH_SHORT, LENGTH_LONG}) int duration) {
        final Toast toast = new Toast(context);
        toast.mText = text;
        toast.mDuration = duration;
        return toast;
    }

    /**
     * Show the view for the specified duration.
     */
    public void show() {
        ((Activity) mContext).getToastManager().enqueueToast(this, mText, mDuration);
    }

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this.  Normally view will disappear on its own
     * after the appropriate duration.
     */
    public void cancel() {
        ((Activity) mContext).getToastManager().cancelToast(this);
    }

    /**
     * Set how long to show the view for.
     *
     * @see #LENGTH_SHORT
     * @see #LENGTH_LONG
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Update the text in a Toast that was previously created using one of the makeText() methods.
     *
     * @param s The new text for the Toast.
     */
    public void setText(@NonNull CharSequence s) {
        mText = s;
    }
}
