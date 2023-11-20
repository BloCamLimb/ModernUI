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

package icyllis.arc3d.core;

/**
 * Marks an object as reference-counted, behavior is implementation-dependent.
 *
 * @see RefCnt
 */
public interface RefCounted {

    @SharedPtr
    static <T extends RefCounted> T move(@SharedPtr T sp) {
        if (sp != null)
            sp.unref();
        return null;
    }

    @SharedPtr
    static <T extends RefCounted> T move(@SharedPtr T sp, @SharedPtr T that) {
        if (sp != null)
            sp.unref();
        return that;
    }

    @SharedPtr
    static <T extends RefCounted> T create(@SharedPtr T that) {
        if (that != null)
            that.ref();
        return that;
    }

    @SharedPtr
    static <T extends RefCounted> T create(@SharedPtr T sp, @SharedPtr T that) {
        if (sp != null)
            sp.unref();
        if (that != null)
            that.ref();
        return that;
    }

    /**
     * Increases the reference count by 1.
     */
    void ref();

    /**
     * Decreases the reference count by 1.
     */
    void unref();
}
