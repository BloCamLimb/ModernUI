/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import java.util.Arrays;

/**
 * A key loaded with custom data.
 * <p>
 * Accepts <code>ImmutableKey</code> as storage key or <code>KeyBuilder</code> as lookup key.
 */
public sealed interface Key permits Key.StorageKey, KeyBuilder {

    final class StorageKey implements Key {

        final int[] data;
        final int hash;

        public StorageKey(KeyBuilder b) {
            data = b.toIntArray();
            hash = Arrays.hashCode(data);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof StorageKey key && Arrays.equals(data, key.data);
        }
    }
}
