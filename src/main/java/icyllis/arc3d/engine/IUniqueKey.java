/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

/**
 * Marker interface for unique resource key, allows for exclusive use of a resource for a use case
 * (AKA "domain"). There are three rules governing the use of unique keys:
 * <ul>
 * <li> Only one resource can have a given unique key at a time. Hence, "unique".</li>
 * <li> A resource can have at most one unique key at a time.</li>
 * <li> Unlike scratch keys, multiple requests for a unique key will return the same
 * resource even if the resource already has refs.</li>
 * </ul>
 * This key type allows a code path to create cached resources for which it is the exclusive user.
 * The code path creates a domain which it sets on its keys. This guarantees that there are no
 * cross-domain collisions.
 * <p>
 * Unique keys preempt scratch keys. While a resource has a unique key it is inaccessible via its
 * scratch key. It can become scratch again if the unique key is removed.
 */
public interface IUniqueKey {

    IUniqueKey copy();

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);
}
