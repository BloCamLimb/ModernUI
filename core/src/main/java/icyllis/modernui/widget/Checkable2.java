/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.widget;

import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

/**
 * A custom checkable interface extending {@link Checkable} to support
 * callback and check group logic.
 *
 * @since 3.12
 */
public interface Checkable2 extends Checkable {

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener);

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes. This callback is used for internal purpose only.
     *
     * @param listener the callback to call on checked state change
     * @hidden
     */
    @ApiStatus.Internal
    default void setInternalOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        setOnCheckedChangeListener(listener);
    }
}
