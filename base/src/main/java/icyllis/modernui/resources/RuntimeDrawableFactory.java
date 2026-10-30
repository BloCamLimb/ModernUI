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

package icyllis.modernui.resources;

import icyllis.modernui.graphics.drawable.Drawable;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Runtime drawable factory.
 *
 * @hidden
 */
@ApiStatus.Internal
@FunctionalInterface
public interface RuntimeDrawableFactory extends RuntimeFactory {

    /**
     * @param resources resources object
     * @param theme     theme object
     * @param cookie    asset pack this resource belongs to
     * @param density   density override, if non-negative
     * @return new drawable instance
     */
    @Nullable
    Drawable create(@NonNull Resources resources, Resources.@Nullable Theme theme, int cookie, int density);
}
