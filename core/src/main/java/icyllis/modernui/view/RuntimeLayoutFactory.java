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

package icyllis.modernui.view;

import icyllis.modernui.core.Context;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.RuntimeFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Runtime layout factory.
 *
 * @hidden
 */
@ApiStatus.Internal
@FunctionalInterface
public interface RuntimeLayoutFactory extends RuntimeFactory {

    /**
     * @param context context object
     * @param theme   theme object
     * @param cookie  asset pack this resource belongs to
     * @param parent  parent view
     * @return new layout instance
     */
    @Nullable
    View create(@NonNull Context context, Resources.@NonNull Theme theme,
                int cookie, @Nullable View parent);
}
