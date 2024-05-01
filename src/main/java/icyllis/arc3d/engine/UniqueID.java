/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import javax.annotation.Nonnull;

/**
 * An object with identity. This can be used to track state changes through
 * reference equality '==', and as keys of {@link java.util.IdentityHashMap}.
 * Used by {@link Resource} and {@link SurfaceProxy}.
 *
 * @see System#identityHashCode(Object)
 */
public final class UniqueID {

    public UniqueID() {
    }

    @Nonnull
    @Override
    public String toString() {
        return "UniqueID@" + Integer.toHexString(hashCode());
    }
}
