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

import icyllis.modernui.api.util.Color3i;

import javax.annotation.Nonnull;

/**
 * Encapsulates the most primitive information to tell the renderer how to
 * render a single glyph in specified text component or string. These props
 * can be parsed by TextFormatting and Style.
 */
@Deprecated
public class StringPropInfo {

    /**
     * RGB color
     */
    public final Color3i color;

    public StringPropInfo(@Nonnull Color3i color) {
        this.color = color;
    }
}
