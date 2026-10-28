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

package icyllis.modernui.graphics;

import icyllis.modernui.annotation.ColorLong;
import icyllis.modernui.util.ColorStateList;

/**
 * Base class of complex color, such as {@link ColorStateList} and {@link GradientColor}.
 */
public abstract class ComplexColor {

    /**
     * @return {@code true} if this {@link ComplexColor} changes color based on state, {@code false}
     * otherwise.
     */
    public abstract boolean isStateful();

    /**
     * @return {@code true} if this {@link ComplexColor} is guaranteed to produce only opaque
     * colors when drawn, regardless of the state.
     */
    public abstract boolean isOpaque();

    /**
     * @return the default color in this {@link ComplexColor}.
     */
    @ColorLong
    public abstract long getDefaultColor();
}
