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

package icyllis.modernui.app;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.view.WindowManager;
import icyllis.modernui.widget.ToastManager;
import org.jetbrains.annotations.ApiStatus;

/**
 * Reserved for future use.
 */
@ApiStatus.Experimental
public class Activity extends Context {

    private volatile ToastManager mToastManager;

    private final Object mThemeLock = new Object();

    private final Resources mResources;
    private Resources.Theme mTheme;
    private ResourceId mThemeResource;

    public Activity() {
        mResources = Resources.getSystem();
    }

    @Override
    public Resources getResources() {
        return mResources;
    }

    @Override
    public void setTheme(@Nullable ResourceId resId) {
        synchronized (mThemeLock) {
            mThemeResource = resId;

            if (mTheme == null) {
                return;
            }

            mTheme.clear();
            mThemeResource = Resources.selectDefaultTheme(mThemeResource);
            mTheme.applyStyle(mThemeResource, true);
        }
    }

    @Override
    public Resources.Theme getTheme() {
        synchronized (mThemeLock) {
            if (mTheme != null) {
                return mTheme;
            }

            mTheme = mResources.newTheme();
            mThemeResource = Resources.selectDefaultTheme(mThemeResource);
            mTheme.applyStyle(mThemeResource, true);

            return mTheme;
        }
    }

    @ApiStatus.Internal
    public ToastManager getToastManager() {
        if (mToastManager != null) {
            return mToastManager;
        }
        synchronized (this) {
            if (mToastManager == null) {
                mToastManager = new ToastManager(this);
            }
        }
        return mToastManager;
    }

    @ApiStatus.Internal
    public WindowManager getWindowManager() {
        return null;
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (WINDOW_SERVICE.equals(name)) {
            return getWindowManager();
        }
        return null;
    }
}
