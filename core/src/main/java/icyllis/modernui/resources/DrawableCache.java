/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.drawable.Drawable;

class DrawableCache extends ThemedResourceCache<Drawable.ConstantState> {

    @Nullable
    public Drawable getInstance(long key, Resources resources, Resources.Theme theme) {
        final Drawable.ConstantState entry = get(key, theme);
        if (entry != null) {
            return entry.newDrawable(resources);
        }

        return null;
    }

    @Override
    protected boolean shouldInvalidateEntry(@NonNull Drawable.ConstantState entry, int configChanges) {
        return Configuration.needNewResources(configChanges, entry.getChangingConfigurations());
    }
}
