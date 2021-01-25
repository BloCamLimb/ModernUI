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

package icyllis.modernui.text;

/**
 * This interface should be added to a span object that should not be copied
 * into a new Spanned when performing a slice or copy operation on the original
 * Spanned it was placed in.
 */
public interface NoCopySpan {

    /**
     * Convenience equivalent for when you would just want a new Object() for
     * a span but want it to be no-copy.  Use this instead.
     */
    class Concrete implements NoCopySpan {
    }
}
