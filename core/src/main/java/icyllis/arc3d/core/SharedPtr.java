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

import java.lang.annotation.*;

/**
 * Denotes something like C++ std::shared_ptr. Differently, "ref" and "unref" are
 * explicitly called in each SP "ctor" and "dtor" to ensure engine work correctly
 * (because GC is not immediate, we don't rely on the lifecycle of Java objects).
 * Some classes may have other methods in addition to the "unref" method (e.g.
 * recycle), depending on how they (e.g. special SPs) are used.
 * <p>
 * When denote a method or parameter, the caller has referenced the object, then
 * transfer ownership. When denote a field, the owner object has to unref it along
 * with object itself. When there is no annotation, it is seen as raw ptr.
 * <p>
 * Denoted classes sometimes implement {@link AutoCloseable}, which is equivalent to
 * "unref". Some classes may not need to be shared, or they may only be shared
 * internally and have a unique owner object, or there are no underlying resources,
 * or there is no need for immediacy. They may implement {@link AutoCloseable},
 * which is equivalent to "free" (or similar methods), you just need to manage
 * their finalizing.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE})
public @interface SharedPtr {
}
