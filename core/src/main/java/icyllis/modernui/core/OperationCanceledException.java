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

package icyllis.modernui.core;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An exception type that is thrown when an operation in progress is canceled.
 */
public class OperationCanceledException extends RuntimeException {

    public OperationCanceledException() {
        this(null);
    }

    public OperationCanceledException(@Nullable String message) {
        super(Objects.toString(message, "The operation has been canceled."));
    }
}
