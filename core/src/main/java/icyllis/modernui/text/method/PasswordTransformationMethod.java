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

package icyllis.modernui.text.method;

import icyllis.modernui.graphics.Rect;
import icyllis.modernui.text.*;
import icyllis.modernui.text.style.UpdateLayout;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class PasswordTransformationMethod implements TransformationMethod, TextWatcher {

    private static final PasswordTransformationMethod sInstance = new PasswordTransformationMethod();

    private static final char DOT = '\u2022';

    private PasswordTransformationMethod() {
    }

    public static PasswordTransformationMethod getInstance() {
        return sInstance;
    }

    @Nonnull
    @Override
    public CharSequence getTransformation(@Nonnull CharSequence source, @Nonnull View view) {
        if (source instanceof Spannable sp) {
            /*
             * Remove any references to other views that may still be
             * attached.  This will happen when you flip the screen
             * while a password field is showing; there will still
             * be references to the old EditText in the text.
             */
            ViewReference[] vr = sp.getSpans(0, sp.length(),
                    ViewReference.class);
            if (vr != null) {
                for (ViewReference r : vr) {
                    sp.removeSpan(r);
                }
            }

            removeVisibleSpans(sp);

            sp.setSpan(new ViewReference(view), 0, 0,
                    Spannable.SPAN_POINT_POINT);
        }

        return new PasswordCharSequence(source);
    }

    @Override
    public void onFocusChanged(@Nonnull View view, @Nonnull CharSequence sourceText,
                               boolean focused, int direction, @Nullable Rect previouslyFocusedRect) {
        if (!focused) {
            if (sourceText instanceof Spannable) {
                removeVisibleSpans((Spannable) sourceText);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s instanceof Spannable sp) {
            ViewReference[] vr = sp.getSpans(0, s.length(),
                    ViewReference.class);
            if (vr == null) {
                return;
            }

            /*
             * There should generally only be one ViewReference in the text,
             * but make sure to look through all of them if necessary in case
             * something strange is going on.  (We might still end up with
             * multiple ViewReferences if someone moves text from one password
             * field to another.)
             */
            View v = null;
            for (int i = 0; v == null && i < vr.length; i++) {
                v = vr[i].get();
            }

            if (v == null) {
                return;
            }

            if (count > 0) {
                removeVisibleSpans(sp);

                if (count == 1) {
                    Visible visible = new Visible(sp, this);
                    sp.setSpan(visible, start, start + count,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    v.postDelayed(visible, 1500);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private static void removeVisibleSpans(@Nonnull Spannable sp) {
        Visible[] old = sp.getSpans(0, sp.length(), Visible.class);
        if (old != null) {
            for (Visible visible : old) {
                sp.removeSpan(visible);
            }
        }
    }

    private static class PasswordCharSequence implements CharSequence, GetChars {

        private final CharSequence mSource;

        public PasswordCharSequence(CharSequence source) {
            mSource = source;
        }

        @Override
        public int length() {
            return mSource.length();
        }

        @Override
        public char charAt(int i) {
            if (mSource instanceof Spanned sp) {
                Visible[] visible = sp.getSpans(0, sp.length(), Visible.class);

                if (visible != null) {
                    for (Visible value : visible) {
                        if (sp.getSpanStart(value.mTransformer) >= 0) {
                            int st = sp.getSpanStart(value);
                            int en = sp.getSpanEnd(value);

                            if (i >= st && i < en) {
                                return mSource.charAt(i);
                            }
                        }
                    }
                }
            }

            return DOT;
        }

        @Nonnull
        @Override
        public CharSequence subSequence(int start, int end) {
            char[] buf = new char[end - start];
            getChars(start, end, buf, 0);
            return new String(buf);
        }

        @Nonnull
        @Override
        public String toString() {
            char[] buf = new char[mSource.length()];
            getChars(0, mSource.length(), buf, 0);
            return new String(buf);
        }

        @Override
        public void getChars(int start, int end, char[] dest, int off) {
            TextUtils.getChars(mSource, start, end, dest, off);

            int count = 0;
            int[] starts = null, ends = null;

            if (mSource instanceof Spanned sp) {
                Visible[] visible = sp.getSpans(0, sp.length(), Visible.class);

                if (visible != null) {
                    count = visible.length;
                    starts = new int[count];
                    ends = new int[count];

                    for (int i = 0; i < count; i++) {
                        if (sp.getSpanStart(visible[i].mTransformer) >= 0) {
                            starts[i] = sp.getSpanStart(visible[i]);
                            ends[i] = sp.getSpanEnd(visible[i]);
                        }
                    }
                }
            }

            for (int i = start; i < end; i++) {
                boolean visible = false;

                for (int a = 0; a < count; a++) {
                    if (i >= starts[a] && i < ends[a]) {
                        visible = true;
                        break;
                    }
                }

                if (!visible) {
                    dest[i - start + off] = DOT;
                }
            }
        }
    }

    private static class Visible implements UpdateLayout, Runnable {

        private final Spannable mText;
        private final PasswordTransformationMethod mTransformer;

        public Visible(Spannable sp, PasswordTransformationMethod ptm) {
            mText = sp;
            mTransformer = ptm;
        }

        @Override
        public void run() {
            mText.removeSpan(this);
        }
    }

    /**
     * Used to stash a reference back to the View in the Editable, so we
     * can use it to check the settings.
     */
    private static class ViewReference extends WeakReference<View> implements NoCopySpan {

        private ViewReference(View v) {
            super(v);
        }
    }
}
