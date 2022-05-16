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

package icyllis.arcui.engine;

import java.lang.annotation.*;

/**
 * Denotes something like C++ std::shared_ptr. Differently, ref() and unref() are
 * explicitly called in each SP "ctor" and "dtor" to ensure DAG work correctly
 * (because GC is not immediate, and we don't rely on the lifecycle of Java objects).
 * <p>
 * When denote a method or parameter, the caller has referenced the object, then
 * transfer ownership. When denote a field, the owner object has to unref it along with
 * object itself. When there is no annotation, it is seen as raw ptr.
 * <p>
 * Not all reference counted objects follow this rule. Non-recycled and non-shared objects
 * directly implement {@link AutoCloseable}, you just need to manage their finalizing.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface SmartPtr {
}
