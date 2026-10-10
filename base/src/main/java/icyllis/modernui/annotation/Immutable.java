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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The class to which this annotation is applied is immutable. This means that
 * its state cannot be seen to change by callers. Of necessity this means that
 * all public fields are final, and that all public final reference fields refer
 * to other immutable objects, and that methods do not publish references to any
 * internal state which is mutable by implementation even if not by design.
 * Immutable objects may still have internal mutable state for purposes of
 * performance optimization; some state variables may be lazily computed, so
 * long as they are computed from immutable state and that callers cannot tell
 * the difference.
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between
 * threads or published without synchronization.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Immutable {
}
