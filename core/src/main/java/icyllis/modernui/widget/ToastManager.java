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
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.ViewGroup;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayDeque;

import static icyllis.modernui.view.ViewConfiguration.dp;

public class ToastManager {

    public static final Marker MARKER = MarkerManager.getMarker("Toast");

    static final ToastManager sInstance = new ToastManager();

    private static final int MAX_TOASTS = 5;

    static final int LONG_DELAY = 3500; // 3.5 seconds
    static final int SHORT_DELAY = 2000; // 2 seconds

    private final ArrayDeque<ToastRecord> mToastQueue = new ArrayDeque<>();

    @GuardedBy("mToastQueue")
    private Toast mCurrentToastShown;

    private final Runnable mDurationReached = this::onDurationReached;

    private final TextView mTextView = new TextView();
    private final FrameLayout.LayoutParams mParams;
    private final Background mBackground = new Background();

    private ToastManager() {
        mParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        mTextView.setMaxLines(2);
        mTextView.setBackground(mBackground);
    }

    @Nullable
    @GuardedBy("mToastQueue")
    private ToastRecord getToastLocked(@Nonnull Toast token) {
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

        mTextView.setText(r.mText);
        mTextView.setTextSize(14);
        mTextView.setMaxWidth(dp(300));
        mTextView.setPadding(dp(16), dp(12), dp(16), dp(12));
        mParams.setMargins(dp(16), 0, dp(16), dp(64));
        mBackground.mRadius = dp(28);
        ModernUI.getInstance().getViewManager().addView(mTextView, mParams);

        int delay = r.getDuration() == Toast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
        delay += 300; // animation
        ArchCore.getUiHandler().postDelayed(mDurationReached, delay);
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

    private void cancelToastLocked(@Nonnull ToastRecord record) {
        ModernUI.getInstance().getViewManager().removeView(mTextView);
        mToastQueue.remove(record);

        if (mToastQueue.size() > 0) {
            showNextToastLocked();
        }
    }

    public void enqueueToast(@Nonnull Toast token, @Nonnull CharSequence text, int duration) {
        synchronized (mToastQueue) {
            ToastRecord record = getToastLocked(token);
            if (record != null) {
                record.update(duration);
            } else {
                if (mToastQueue.size() >= MAX_TOASTS) {
                    ModernUI.LOGGER.error(MARKER, "App has already queued {} toasts. Not showing more.",
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

    public void cancelToast(@Nonnull Toast token) {
        synchronized (mToastQueue) {
            ToastRecord r = getToastLocked(token);
            if (r != null) {
                cancelToastLocked(r);
            } else {
                ModernUI.LOGGER.warn(MARKER, "Toast already cancelled. token={}", token);
            }
        }
    }

    private static class Background extends Drawable {

        private float mRadius;

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            paint.setColor(0xC0000000);
            Rect b = getBounds();
            canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, mRadius, paint);
        }
    }
}
