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
 * Copy vanilla text from {@link IReorderingProcessor}
 */
public class ReorderTextHandler {

    private final MutableString dynamic = new MutableString();

    @Nullable
    private Style last;

    private IAction action;

    // composite
    private final ICharacterConsumer sink = new ICharacterConsumer() {

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            if (style != last) {
                if (!dynamic.isEmpty() && last != null) {
                    if (action.handle(dynamic, last)) {
                        dynamic.clear();
                        last = style;
                        // stop
                        return false;
                    }
                }
                dynamic.clear();
                last = style;
            }
            dynamic.addCodePoint(codePoint);
            // continue
            return true;
        }
    };

    /**
     * @return {@code false} if action stopped on the way
     */
    public boolean handle(@Nonnull IReorderingProcessor sequence, IAction action) {
        this.action = action;
        if (sequence.accept(sink)) {
            // iteration completed
            return finish();
        }
        // stopped
        return false;
    }

    private boolean finish() {
        if (!dynamic.isEmpty() && last != null) {
            if (action.handle(dynamic, last)) {
                dynamic.clear();
                last = null;
                return false;
            }
        }
        dynamic.clear();
        last = null;
        return true;
    }

    @FunctionalInterface
    public interface IAction {

        /**
         * @return {@code true} to stop action, {@code false} to continue
         */
        boolean handle(CharSequence t, Style s);
    }
}
