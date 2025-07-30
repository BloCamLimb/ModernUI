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

package icyllis.modernui.widget;

import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.app.Activity;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.Log;
import icyllis.modernui.view.*;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayDeque;

@ApiStatus.Internal
public final class ToastManager {

    static final Marker MARKER = MarkerFactory.getMarker("Toast");

    private static final int MAX_TOASTS = 5;

    static final int LONG_DELAY = 3500; // 3.5 seconds
    static final int SHORT_DELAY = 2000; // 2 seconds

    private final WindowManager mWindowManager;

    private final ArrayDeque<ToastRecord> mToastQueue = new ArrayDeque<>(MAX_TOASTS);

    @GuardedBy("mToastQueue")
    private Toast mCurrentToastShown;

    private final Runnable mDurationReached = this::onDurationReached;

    private final TextView mTextView;
    private final WindowManager.LayoutParams mParams;

    public ToastManager(Activity activity) {
        mWindowManager = activity.getWindowManager();
        mTextView = new TextView(activity);
        mParams = new WindowManager.LayoutParams();
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        mTextView.setMaxLines(2);
    }

    @Nullable
    @GuardedBy("mToastQueue")
    private ToastRecord getToastLocked(@NonNull Toast token) {
        for (ToastRecord r : mToastQueue) {
            if (r.mToken == token) {
                return r;
            }
        }
        return null;
    }

    private void showNextToastLocked() {
        if (mCurrentToastShown != null) {
            return;
        }
        ToastRecord r = mToastQueue.getFirst();

        final TypedValue value = new TypedValue();
        final Resources.Theme theme = mTextView.getContext().getTheme();
        mTextView.setText(r.mText);
        mTextView.setTextSize(14);
        if (theme.resolveAttribute(R.ns, R.attr.textColorPrimary, value, true))
            mTextView.setTextColor(theme.getResources().loadColorStateList(value, theme));
        mTextView.setTypeface(ModernUI.getSelectedTypeface());
        mTextView.setMaxWidth(mTextView.dp(300));
        mTextView.setPadding(mTextView.dp(16), mTextView.dp(12), mTextView.dp(16), mTextView.dp(12));
        mParams.y = mTextView.dp(64);
        ShapeDrawable bg = new ShapeDrawable();
        if (theme.resolveAttribute(R.ns, R.attr.colorBackground, value, true))
            bg.setColor(value.data);
        bg.setCornerRadius(mTextView.dp(28));
        mTextView.setBackground(bg);
        mTextView.setElevation(mTextView.dp(2));
        mWindowManager.addView(mTextView, mParams);

        int delay = r.getDuration() == Toast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
        delay += 300; // animation
        Core.getUiHandlerAsync().postDelayed(mDurationReached, delay);
        mCurrentToastShown = r.mToken;
    }

    private void onDurationReached() {
        synchronized (mToastQueue) {
            if (mCurrentToastShown != null) {
                ToastRecord record = getToastLocked(mCurrentToastShown);
                mCurrentToastShown = null;
                if (record != null) {
                    cancelToastLocked(record);
                }
            }
        }
    }

    private void cancelToastLocked(@NonNull ToastRecord record) {
        mWindowManager.removeView(mTextView);
        mToastQueue.remove(record);

        if (mToastQueue.size() > 0) {
            showNextToastLocked();
        }
    }

    public void enqueueToast(@NonNull Toast token, @NonNull CharSequence text, int duration) {
        synchronized (mToastQueue) {
            ToastRecord record = getToastLocked(token);
            if (record != null) {
                record.update(duration);
            } else {
                if (mToastQueue.size() >= MAX_TOASTS) {
                    Log.LOGGER.error(MARKER, "System has already queued {} toasts. Not showing more.",
                            mToastQueue.size());
                    return;
                }
                record = new ToastRecord(token, text, duration);
                mToastQueue.addLast(record);
            }
            if (mToastQueue.size() == 1) {
                showNextToastLocked();
            }
        }
    }

    public void cancelToast(@NonNull Toast token) {
        synchronized (mToastQueue) {
            ToastRecord r = getToastLocked(token);
            if (r != null) {
                cancelToastLocked(r);
            } else {
                Log.LOGGER.warn(MARKER, "Toast already cancelled. token={}", token);
            }
        }
    }

    static final class ToastRecord {

        public final Toast mToken;
        public final CharSequence mText;

        private int mDuration;

        ToastRecord(Toast token, CharSequence text, int duration) {
            mToken = token;
            mText = text;
            mDuration = duration;
        }

        /**
         * Returns the duration of this toast, which can be {@link Toast#LENGTH_SHORT}
         * or {@link Toast#LENGTH_LONG}.
         */
        public int getDuration() {
            return mDuration;
        }

        /**
         * Updates toast duration.
         */
        public void update(int duration) {
            mDuration = duration;
        }
    }
}
