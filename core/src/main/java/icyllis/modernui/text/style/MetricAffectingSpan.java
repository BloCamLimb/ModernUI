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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text.style;

import icyllis.modernui.text.TextPaint;

import javax.annotation.Nonnull;

/**
 * The classes that affect character-level text formatting in a way that
 * changes the metric of characters extend this class.
 */
public abstract class MetricAffectingSpan extends CharacterStyle implements UpdateLayout {

    @Override
    public void updateDrawState(@Nonnull TextPaint paint) {
        updateMeasureState(paint);
    }

    /**
     * Classes that extend MetricAffectingSpan implement this method to update the text formatting
     * in a way that can change the width or height of characters.
     *
     * @param paint the paint used for measuring the text
     */
    public abstract void updateMeasureState(@Nonnull TextPaint paint);

    /**
     * Returns "this" for most MetricAffectingSpans, but for
     * MetricAffectingSpans that were generated by {@link #wrap},
     * returns the underlying MetricAffectingSpan.
     */
    @Override
    public MetricAffectingSpan getUnderlying() {
        return this;
    }

    /**
     * A Passthrough MetricAffectingSpan is one that
     * passes {@link #updateDrawState} and {@link #updateMeasureState}
     * calls through to the specified MetricAffectingSpan
     * while still being a distinct object,
     * and is therefore able to be attached to the same Spannable
     * to which the specified MetricAffectingSpan is already attached.
     */
    static class Passthrough extends MetricAffectingSpan {

        private final MetricAffectingSpan mStyle;

        /**
         * Creates a new Passthrough of the specified MetricAffectingSpan.
         */
        Passthrough(@Nonnull MetricAffectingSpan cs) {
            mStyle = cs;
        }

        /**
         * Passes updateMeasureState through to the underlying MetricAffectingSpan.
         *
         * @param paint
         */
        @Override
        public void updateMeasureState(@Nonnull TextPaint paint) {
            mStyle.updateMeasureState(paint);
        }

        /**
         * Returns the MetricAffectingSpan underlying this one, or the one
         * underlying it if it too is a Passthrough.
         */
        @Override
        public MetricAffectingSpan getUnderlying() {
            return mStyle.getUnderlying();
        }
    }
}
