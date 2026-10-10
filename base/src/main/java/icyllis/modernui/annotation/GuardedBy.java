/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The field or method to which this annotation is applied can only be accessed
 * when holding a particular lock, which may be a built-in (synchronization)
 * lock, or may be an explicit {@link java.util.concurrent.locks.Lock}.
 * <p>
 * The argument determines which lock guards the annotated field or method:
 * <ul>
 * <li>this : The string literal "this" means that this field is guarded by the class in which it is defined.
 * <li>class-name.this : For inner classes, it may be necessary to disambiguate 'this';
 * the class-name.this designation allows you to specify which 'this' reference is intended
 * <li>itself : For reference fields only; the object to which the field refers.
 * <li>field-name : The lock object is referenced by the (instance or static) field specified by field-name.
 * <li>class-name.field-name : The lock object is reference by the static field specified by class-name.field-name.
 * <li>method-name() : The lock object is returned by calling the named nil-ary method.
 * <li>class-name.class : The Class object for the specified class should be used as the lock object.
 * </ul>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD,
        ElementType.METHOD})
public @interface GuardedBy {
    String value();
}
