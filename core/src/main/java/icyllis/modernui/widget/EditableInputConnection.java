/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.widget;

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.NoCopySpan;
import icyllis.modernui.text.Selection;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.SpannableString;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.style.UnderlineSpan;
import icyllis.modernui.view.textinput.InputConnection;

import java.util.List;

public class EditableInputConnection implements InputConnection {

    static final Object COMPOSING = new NoCopySpan
            .Concrete();

    private final TextView mTextView;

    public EditableInputConnection(TextView textView) {
        mTextView = textView;
    }

    @Override
    public boolean setComposingText(CharSequence text, int selStart, int selEnd) {
        Editable content = mTextView.getEditableText();
        if (content == null) {
            return true;
        }
        int start = content.getSpanStart(COMPOSING);
        int end = content.getSpanEnd(COMPOSING);
        if (start >= 0 && end >= 0) {
            content.removeSpan(COMPOSING);
            List<?> sps = content.getSpans(start, end, null);
            for (int i=sps.size()-1; i>=0; i--) {
                Object o = sps.get(i);
                if ((content.getSpanFlags(o)& Spanned.SPAN_COMPOSING) != 0) {
                    content.removeSpan(o);
                }
            }
        } else {
            start = Selection.getSelectionStart(content);
            end = Selection.getSelectionEnd(content);
            if (start < 0)
                start = 0;
            if (end < 0)
                end = 0;
        }
        if (end < start) {
            var __ = start;start = end;end = __;
        }

        Spannable sp;
        if (text instanceof Spannable) {
            sp = (Spannable) text;
            List<?> sps = sp.getSpans(0, sp.length(), null);
            for (int i=sps.size()-1; i>=0; i--) {
                final Object o = sps.get(i);
                final int fl = sp.getSpanFlags(o);
                if ((fl & (Spanned.SPAN_COMPOSING | Spanned.SPAN_POINT_MARK_MASK))
                        != (Spanned.SPAN_COMPOSING | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)) {
                    sp.setSpan(
                            o,
                            sp.getSpanStart(o),
                            sp.getSpanEnd(o),
                            (fl & ~Spanned.SPAN_POINT_MARK_MASK)
                                    | Spanned.SPAN_COMPOSING
                                    | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } else {
            sp = new SpannableString(text);
            sp.setSpan(new UnderlineSpan(), 0, sp.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
        }
        sp.setSpan(COMPOSING, 0, sp.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
        content.replace(start, end, sp);

        int newSelStart = start + selStart;
        int newSelEnd = start + selEnd;
        newSelStart = MathUtil.clamp(newSelStart, 0, content.length());
        newSelEnd = Math.max(newSelEnd, newSelStart);
        Selection.setSelection(content, newSelStart, newSelEnd);
        return true;
    }

    @Override
    public boolean finishComposingText() {
        Editable content = mTextView.getEditableText();
        if (content == null) {
            return true;
        }
        int start = content.getSpanStart(COMPOSING);
        int end = content.getSpanEnd(COMPOSING);
        if (start >= 0 && end >= 0) {
            content.removeSpan(COMPOSING);
            List<?> sps = content.getSpans(start, end, null);
            for (int i=sps.size()-1; i>=0; i--) {
                Object o = sps.get(i);
                if ((content.getSpanFlags(o)& Spanned.SPAN_COMPOSING) != 0) {
                    content.removeSpan(o);
                }
            }
        }
        return true;
    }

    @Override
    public boolean commitText(CharSequence text) {
        Editable content = mTextView.getEditableText();
        if (content == null) {
            return true;
        }
        int start = content.getSpanStart(COMPOSING);
        int end = content.getSpanEnd(COMPOSING);
        if (start >= 0 && end >= 0) {
            content.removeSpan(COMPOSING);
            List<?> sps = content.getSpans(start, end, null);
            for (int i=sps.size()-1; i>=0; i--) {
                Object o = sps.get(i);
                if ((content.getSpanFlags(o)& Spanned.SPAN_COMPOSING) != 0) {
                    content.removeSpan(o);
                }
            }
        } else {
            start = Selection.getSelectionStart(content);
            end = Selection.getSelectionEnd(content);
            if (start < 0)
                start = 0;
            if (end < 0)
                end = 0;
        }
        if (end < start) {
            var __ = start;start = end;end = __;
        }

        Selection.setSelection(content, end);
        content.replace(start, end, text);
        return true;
    }

    @Override
    public void closeConnection() {
        finishComposingText();
    }
}
