/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.text.method;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.GetChars;
import icyllis.modernui.text.NoCopySpan;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.text.style.UpdateLayout;
import icyllis.modernui.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

// Modified, with some optimizations
@SuppressWarnings("ForLoopReplaceableByForEach")
public class PasswordTransformationMethod implements TransformationMethod, TextWatcher {

    private static final PasswordTransformationMethod sInstance = new PasswordTransformationMethod();

    private static final char DOT = '\u2022';

    protected PasswordTransformationMethod() {
    }

    @NonNull
    public static PasswordTransformationMethod getInstance() {
        return sInstance;
    }

    @NonNull
    @Override
    public CharSequence getTransformation(@NonNull CharSequence source, @NonNull View view) {
        if (source instanceof Spannable sp) {
            /*
             * Remove any references to other views that may still be
             * attached.  This will happen when you flip the screen
             * while a password field is showing; there will still
             * be references to the old EditText in the text.
             */
            List<ViewReference> vr = sp.getSpans(0, sp.length(),
                    ViewReference.class);
            for (int i = 0; i < vr.size(); i++) {
                sp.removeSpan(vr.get(i));
            }

            removeVisibleSpans(sp);

            sp.setSpan(new ViewReference(view), 0, 0,
                    Spannable.SPAN_POINT_POINT);
        }

        return new PasswordCharSequence(source);
    }

    @Override
    public void onFocusChanged(@NonNull View view, @NonNull CharSequence sourceText,
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
            List<ViewReference> vr = sp.getSpans(0, s.length(),
                    ViewReference.class);
            if (vr.isEmpty()) {
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
            for (int i = 0; v == null && i < vr.size(); i++) {
                v = vr.get(i).get();
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

    private static void removeVisibleSpans(@NonNull Spannable sp) {
        List<Visible> old = sp.getSpans(0, sp.length(), Visible.class);
        for (int i = 0; i < old.size(); i++) {
            sp.removeSpan(old.get(i));
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
                List<Visible> visible = sp.getSpans(0, sp.length(), Visible.class);

                for (int j = 0; j < visible.size(); j++) {
                    Visible value = visible.get(j);
                    if (sp.getSpanStart(value.mTransformer) >= 0) {
                        int st = sp.getSpanStart(value);
                        int en = sp.getSpanEnd(value);

                        if (i >= st && i < en) {
                            return mSource.charAt(i);
                        }
                    }
                }
            }

            return DOT;
        }

        @NonNull
        @Override
        public CharSequence subSequence(int start, int end) {
            int len = end - start;
            char[] s = TextUtils.obtain(len);
            try {
                getChars(start, end, s, 0);
                return new String(s, 0, len);
            } finally {
                TextUtils.recycle(s);
            }
        }

        @NonNull
        @Override
        public String toString() {
            int len = length();
            char[] s = TextUtils.obtain(len);
            try {
                getChars(0, len, s, 0);
                return new String(s, 0, len);
            } finally {
                TextUtils.recycle(s);
            }
        }

        @Override
        public void getChars(int start, int end, char[] dest, int off) {
            TextUtils.getChars(mSource, start, end, dest, off);

            int count = 0;
            int[] starts = null, ends = null;

            if (mSource instanceof Spanned sp) {
                List<Visible> visible = sp.getSpans(0, sp.length(), Visible.class);

                count = visible.size();
                starts = new int[count];
                ends = new int[count];

                for (int i = 0; i < count; i++) {
                    Visible value = visible.get(i);
                    if (sp.getSpanStart(value.mTransformer) >= 0) {
                        starts[i] = sp.getSpanStart(value);
                        ends[i] = sp.getSpanEnd(value);
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
