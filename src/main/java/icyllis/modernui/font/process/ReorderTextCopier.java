/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.font.process;

import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A very hacky way to copy Mojang's (non) reordered text
 * There may be bugs
 */
public class ReorderTextCopier implements ICharacterConsumer {

    private Behavior behavior;

    private final MutableString mutableString = new MutableString();

    @Nullable
    private Style lastStyle;

    /**
     * @return {@code false} if action stopped
     */
    public boolean copyAndConsume(@Nonnull IReorderingProcessor reorderingProcessor, Behavior behavior) {
        this.behavior = behavior;
        if (!reorderingProcessor.accept(this)) {
            // stopped
            return false;
        }
        return finish();
    }

    @Override
    public boolean accept(int index, @Nonnull Style style, int codePoint) {
        if (style != lastStyle) {
            if (!mutableString.chars.isEmpty() && lastStyle != null) {
                if (behavior.consumeText(mutableString, lastStyle)) {
                    mutableString.chars.clear();
                    lastStyle = style;
                    // stop
                    return false;
                }
            }
            mutableString.chars.clear();
            lastStyle = style;
        }
        if (Character.isBmpCodePoint(codePoint)) {
            mutableString.chars.add((char) codePoint);
        } else {
            mutableString.chars.add(Character.highSurrogate(codePoint));
            mutableString.chars.add(Character.lowSurrogate(codePoint));
        }
        // continue
        return true;
    }

    private boolean finish() {
        if (!mutableString.chars.isEmpty() && lastStyle != null) {
            if (behavior.consumeText(mutableString, lastStyle)) {
                mutableString.chars.clear();
                lastStyle = null;
                return false;
            }
        }
        mutableString.chars.clear();
        lastStyle = null;
        return true;
    }

    @FunctionalInterface
    public interface Behavior {
        /**
         * @return {@code true} to stop action
         */
        boolean consumeText(CharSequence t, Style s);
    }
}
