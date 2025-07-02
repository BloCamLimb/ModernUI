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

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.Spanned;
import icyllis.modernui.text.TextPaint;

/**
 * The classes that affect character-level text formatting extend this
 * class.
 * <br>Most extend its subclass {@link MetricAffectingSpan}, but simple
 * ones may just implement {@link UpdateAppearance}.
 */
// Added: Cloneable
public abstract class CharacterStyle implements Cloneable {

    public abstract void updateDrawState(@NonNull TextPaint paint);

    /**
     * A given {@link CharacterStyle} can only be applied to a single region
     * of a given {@link Spanned}.
     * <br>If you need to attach the same {@link CharacterStyle} to multiple
     * regions, you can use this method to wrap it with a new object that
     * will have the same effect but be a distinct object so that it can
     * also be attached without conflict.
     */
    @NonNull
    public static CharacterStyle wrap(@NonNull CharacterStyle cs) {
        // Modern UI changed: original Passthrough class won't work with Spanned.getSpans()
        // with a specified class type, clone it instead
        return cs.clone();
    }

    /**
     * A given {@link CharacterStyle} can only be applied to a single region
     * of a given {@link Spanned}.
     * <br>If you need to attach the same {@link CharacterStyle} to multiple
     * regions, you can use this method to clone a new object that
     * will have the same effect but be a distinct object so that it can
     * also be attached without conflict.
     */
    @Override
    public CharacterStyle clone() {
        try {
            return (CharacterStyle) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
