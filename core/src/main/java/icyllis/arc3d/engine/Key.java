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

package icyllis.arc3d.engine;

import java.util.Arrays;

/**
 * A key loaded with custom data.
 * <p>
 * Accepts <code>Key.Storage</code> as storage key or <code>KeyBuilder</code> as lookup key.
 */
public sealed interface Key permits Key.Storage, KeyBuilder {

    final class Storage implements Key {

        final int[] data;
        final int hash;

        Storage(int[] data) {
            this.data = data;
            hash = Arrays.hashCode(data);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Storage key && Arrays.equals(data, key.data);
        }
    }
}
