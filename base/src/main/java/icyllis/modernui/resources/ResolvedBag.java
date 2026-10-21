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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.Immutable;
import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@Immutable
@ApiStatus.Internal
public final class ResolvedBag {
    public static final int COLUMN_TYPE = 0;
    public static final int COLUMN_DATA = 1;
    public static final int COLUMN_COOKIE = 2;
    public static final int VALUE_COLUMNS = 3;

    public final int typeSpecFlags;

    /*
     * This holds attribute names, sorted in lexicographic order.
     */
    public final String[] keys;
    /*
        struct Value {
            uint32_t type;
            uint32_t data;
            int32_t cookie;
        }
     */
    public final int[] values;

    public ResolvedBag(String[] keys, int[] values, int typeSpecFlags) {
        this.keys = keys;
        this.values = values;
        this.typeSpecFlags = typeSpecFlags;
    }

    public int type(int index) {
        return values[index * VALUE_COLUMNS + COLUMN_TYPE];
    }

    public int data(int index) {
        return values[index * VALUE_COLUMNS + COLUMN_DATA];
    }

    public int cookie(int index) {
        return values[index * VALUE_COLUMNS + COLUMN_COOKIE];
    }

    public int getEntryCount() {
        assert (values.length / VALUE_COLUMNS) == keys.length;
        return keys.length;
    }
}
