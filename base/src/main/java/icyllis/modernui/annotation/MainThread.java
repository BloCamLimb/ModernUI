/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

import java.lang.annotation.*;

/**
 * Denotes that the annotated method should only be called on the main thread,
 * the thread is used for handling events from the window system.<br>
 * Main thread can sometimes be the same thread as render thread.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.TYPE,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE})
public @interface MainThread {
}
