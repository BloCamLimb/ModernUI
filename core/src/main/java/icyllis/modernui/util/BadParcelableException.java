/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

/**
 * Exception thrown when a {@link Parcelable} is malformed or otherwise invalid.
 * <p>
 * This is typically encountered when a custom {@link Parcelable} object is
 * passed to another process that doesn't have the same {@link Parcelable} class
 * in its {@link ClassLoader}.
 */
public class BadParcelableException extends RuntimeException {

    public BadParcelableException(String msg) {
        super(msg);
    }

    public BadParcelableException(Exception cause) {
        super(cause);
    }

    public BadParcelableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
