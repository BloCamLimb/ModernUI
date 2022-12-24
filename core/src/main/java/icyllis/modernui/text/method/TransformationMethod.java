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
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TextView uses TransformationMethods to do things like replacing the
 * characters of passwords with dots, or keeping the newline characters
 * from causing line breaks in single-line text fields.
 */
public interface TransformationMethod {

    /**
     * Returns a CharSequence that is a transformation of the source text --
     * for example, replacing each character with a dot in a password field.
     * Beware that the returned text must be exactly the same length as
     * the source text, and that if the source text is Editable, the returned
     * text must mirror it dynamically instead of doing a one-time copy.
     */
    @Nonnull
    CharSequence getTransformation(@Nonnull CharSequence source, @Nonnull View view);

    /**
     * This method is called when the TextView that uses this
     * TransformationMethod gains or loses focus.
     */
    void onFocusChanged(@Nonnull View view, @Nonnull CharSequence sourceText,
                        boolean focused, int direction, @Nullable Rect previouslyFocusedRect);
}
