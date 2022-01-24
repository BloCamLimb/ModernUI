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

package icyllis.modernui.textmc;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Copy vanilla text from {@link FormattedCharSequence}
 */
@Deprecated
class ReorderTextHandler {

    private final CharSequenceBuilder mBuffer = new CharSequenceBuilder();

    @Nullable
    private Style mLast;

    private IConsumer mConsumer;

    // composite
    private final FormattedCharSink mSink = new FormattedCharSink() {

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            if (style != mLast) {
                if (!mBuffer.isEmpty() && mLast != null) {
                    if (mConsumer.handle(mBuffer, mLast)) {
                        mBuffer.clear();
                        mLast = style;
                        // stop
                        return false;
                    }
                }
                mBuffer.clear();
                mLast = style;
            }
            mBuffer.addCodePoint(codePoint);
            // continue
            return true;
        }
    };

    /**
     * @return {@code false} if action stopped on the way
     */
    public boolean handle(@Nonnull FormattedCharSequence sequence, IConsumer consumer) {
        this.mConsumer = consumer;
        if (sequence.accept(mSink)) {
            // iteration completed
            return finish();
        }
        // stopped
        return false;
    }

    private boolean finish() {
        if (!mBuffer.isEmpty() && mLast != null) {
            if (mConsumer.handle(mBuffer, mLast)) {
                mBuffer.clear();
                mLast = null;
                return false;
            }
        }
        mBuffer.clear();
        mLast = null;
        return true;
    }

    @FunctionalInterface
    public interface IConsumer {

        /**
         * @return {@code true} to stop action, {@code false} to continue
         */
        boolean handle(CharSequence t, Style s);
    }
}
