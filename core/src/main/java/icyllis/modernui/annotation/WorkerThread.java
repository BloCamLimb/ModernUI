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

package icyllis.modernui.annotation;

import java.lang.annotation.*;

/**
 * Denotes that the annotated method should only be called on a worker thread.
 * If the annotated element is a class, then all methods in the class should be called
 * on a worker thread.
 * <p>
 * Example:
 * <pre>{@code
 *  @WorkerThread
 *  protected abstract FilterResults performFiltering(CharSequence constraint);
 * }</pre>
 *
 * @since 3.7
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.TYPE,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE})
public @interface WorkerThread {
}
