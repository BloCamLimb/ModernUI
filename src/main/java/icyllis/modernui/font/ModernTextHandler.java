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

package icyllis.modernui.font;

import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;

/**
 * Handle line breaks, get text width, etc.
 */
public class ModernTextHandler extends CharacterManager {

    /**
     * Constructor
     *
     * @param widthRetriever retrieve char width with given codePoint
     */
    public ModernTextHandler(ICharWidthProvider widthRetriever) {
        super(widthRetriever);
    }

    /**
     * Wrap lines
     *
     * @param text      text to handle
     * @param wrapWidth max width of each line
     * @param style     style for the text
     * @param retainEnd retain the last word on each line
     * @param acceptor  accept results, params{current style, start index (inclusive), end index (exclusive)}
     */
    @Override
    public void func_238353_a_(String text, int wrapWidth, @Nonnull Style style, boolean retainEnd, @Nonnull ISliceAcceptor acceptor) {
        super.func_238353_a_(text, wrapWidth, style, retainEnd, acceptor);
    }
}
