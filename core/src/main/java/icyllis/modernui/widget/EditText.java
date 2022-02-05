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

import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Selection;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.text.method.ArrowKeyMovementMethod;
import icyllis.modernui.view.Gravity;

import javax.annotation.Nonnull;

/*
 * This is supposed to be a *very* thin veneer over TextView.
 * Do not make any changes here that do anything that a TextView
 * with a key listener and a movement method wouldn't do!
 */

/**
 * A user interface element for entering and modifying text.
 * <p>
 * You can receive callbacks as a user changes text by
 * adding a {@link icyllis.modernui.text.TextWatcher} to the edit text.
 * This is useful when you want to add auto-save functionality as changes are made,
 * or validate the format of user input, for example.
 * You add a text watcher using the {@link TextView#addTextChangedListener} method.
 * <p>
 * To create an {@link EditText} (an editable TextView):
 * <pre>
 *  EditText editText = new EditText();
 * </pre>
 * <p>
 * To make it a single line EditText (a text field):
 * <pre>
 *  editText.setSingleLine();
 * </pre>
 * To make it a password EditText:
 * <pre>
 *  editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
 * </pre>
 */
public class EditText extends TextView {

    public EditText() {
        setText("", BufferType.EDITABLE);
        setMovementMethod(ArrowKeyMovementMethod.getInstance());
        setFocusableInTouchMode(true);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    @Nonnull
    @Override
    public Editable getText() {
        CharSequence text = super.getText();
        if (text instanceof Editable) {
            return (Editable) super.getText();
        }
        super.setText(text, BufferType.EDITABLE);
        return (Editable) super.getText();
    }

    @Override
    public void setText(@Nonnull CharSequence text, @Nonnull BufferType type) {
        super.setText(text, BufferType.EDITABLE);
    }

    /**
     * Convenience for {@link Selection#setSelection(Spannable, int, int)}.
     */
    public void setSelection(int start, int stop) {
        Selection.setSelection(getText(), start, stop);
    }

    /**
     * Convenience for {@link Selection#setSelection(Spannable, int)}.
     */
    public void setSelection(int index) {
        Selection.setSelection(getText(), index);
    }

    /**
     * Convenience for {@link Selection#selectAll}.
     */
    public void selectAll() {
        Selection.selectAll(getText());
    }

    /**
     * Convenience for {@link Selection#extendSelection(Spannable, int)}.
     */
    public void extendSelection(int index) {
        Selection.extendSelection(getText(), index);
    }

    /**
     * Causes words in the text that are longer than the view's width to be ellipsized instead of
     * broken in the middle. {@link TextUtils.TruncateAt#MARQUEE
     * TextUtils.TruncateAt#MARQUEE} is not supported.
     *
     * @param ellipsis Type of ellipsis to be applied.
     * @throws IllegalArgumentException When the value of <code>ellipsis</code> parameter is
     *                                  {@link TextUtils.TruncateAt#MARQUEE}.
     * @see TextView#setEllipsize(TextUtils.TruncateAt)
     */
    @Override
    public void setEllipsize(TextUtils.TruncateAt ellipsis) {
        if (ellipsis == TextUtils.TruncateAt.MARQUEE) {
            throw new IllegalArgumentException("EditText cannot use the ellipsize mode "
                    + "TextUtils.TruncateAt.MARQUEE");
        }
        super.setEllipsize(ellipsis);
    }
}
