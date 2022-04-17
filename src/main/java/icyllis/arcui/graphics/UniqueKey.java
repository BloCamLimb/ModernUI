/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.graphics;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A key that allows for exclusive use of a resource for a use case (AKA "domain"). There are three
 * rules governing the use of unique keys:
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
public final class UniqueKey extends ResourceKey {

    // 0 is invalid
    private static final AtomicInteger sNextDomain = new AtomicInteger(1);

    /**
     * @return the next unique key domain
     */
    public static int createDomain() {
        return sNextDomain.getAndIncrement();
    }

    /**
     * Creates an invalid unique key. It must be initialized before use.
     */
    public UniqueKey() {
    }

    public UniqueKey(UniqueKey key) {
        super(key);
    }

    public void set(UniqueKey key) {
        super.set(key);
    }
}
