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

package icyllis.arcui;

/**
 * Manages the lifetime of all GpuResource instances.
 * <p>
 * Resources may optionally have two types of keys:
 * <ol>
 *  <li> <b>Shared key</b>.
 *      This is for resources whose allocations are cached but not their contents.
 *      Multiple resources may have the same shared key. This is so a caller can have two
 *      resource instances with the same properties (e.g. multi-pass rendering that ping-pongs
 *      between two temporary surfaces). The shared key is set at resource creation time and
 *      should never change. Resources need not have a shared key.
 *  <li> <b>Unique key</b>.
 *      This key's meaning is specific to the domain that created the key. Only one
 *      resource may have a given unique key. The unique key can be set, cleared, or changed
 *      anytime after resource creation.
 * </ol>
 * <p>
 * A unique key always takes precedence over a shared key when a resource has both types of keys.
 * If a resource has neither key type then it will be deleted as soon as the last reference to it
 * is dropped.
 */
public class ResourceCache implements AutoCloseable {

    @Override
    public void close() {

    }
}
