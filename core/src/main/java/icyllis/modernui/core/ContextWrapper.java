/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Proxying implementation of Context that simply delegates all of its calls to
 * another Context.  Can be subclassed to modify behavior without changing
 * the original Context.
 */
public class ContextWrapper extends Context {
    Context mBase;

    public ContextWrapper(Context base) {
        mBase = base;
    }

    /**
     * Set the base context for this ContextWrapper.  All calls will then be
     * delegated to the base context.  Throws
     * IllegalStateException if a base context has already been set.
     *
     * @param base The new base context for this wrapper.
     */
    protected void attachBaseContext(Context base) {
        if (mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBase = base;
    }

    protected void detachBaseContext() {
        mBase = null;
    }

    /**
     * @return the base context as set by the constructor or setBaseContext
     */
    public Context getBaseContext() {
        return mBase;
    }

    @Override
    public Resources getResources() {
        return mBase.getResources();
    }

    @Deprecated
    @Override
    public void setTheme(@Nullable @StyleRes ResourceId resId) {
        mBase.setTheme(resId);
    }

    @Deprecated
    @Override
    public Resources.Theme getTheme() {
        return mBase.getTheme();
    }

    @Nullable
    @Override
    public Object getSystemService(@NonNull String name) {
        return mBase.getSystemService(name);
    }
}
