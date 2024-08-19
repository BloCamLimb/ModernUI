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
 * Marker interface for scratch resource keys. There are three important rules about scratch keys:
 * <ul>
 * <li> Multiple resources can share the same scratch key. Therefore resources assigned the same
 * scratch key should be interchangeable with respect to the code that uses them.</li>
 * <li> A resource can have at most one scratch key and it is set at resource creation by the
 * resource itself.</li>
 * <li> When a scratch resource is referenced it will not be returned from the
 * cache for a subsequent cache request until all refs are released. This facilitates using
 * a scratch key for multiple render-to-texture scenarios. An example is a separable blur:</li>
 */
public interface IResourceKey {

    /**
     * Can the resource be held by multiple users at the same time?
     * For example, pipelines, samplers, etc.
     *
     * @return true if shareable, false if scratch
     */
    default boolean isShareable() {
        return false;
    }

    IResourceKey copy();

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);
}
