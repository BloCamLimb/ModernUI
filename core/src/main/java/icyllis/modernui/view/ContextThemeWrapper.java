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

package icyllis.modernui.view;

import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.ContextWrapper;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.ThemeKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A context wrapper that allows you to modify or replace the theme of the
 * wrapped context.
 *
 * @deprecated theme will no longer be part of Context object, you should create a Theme object yourself,
 * there's no need to create a ContextWrapper
 */
@Deprecated(forRemoval = true)
public class ContextThemeWrapper extends ContextWrapper {
    private final Object mThemeLock = new Object();
    private ResourceId mThemeResource;
    private Resources.Theme mTheme;
    private Resources mResources;

    /**
     * Creates a new context wrapper with no theme and no base context.
     * <p class="note">
     * <strong>Note:</strong> A base context <strong>must</strong> be attached
     * using {@link #attachBaseContext(Context)} before calling any other
     * method on the newly constructed context wrapper.
     */
    public ContextThemeWrapper() {
        super(null);
    }

    /**
     * Creates a new context wrapper with the specified theme.
     * <p>
     * The specified theme will be applied on top of the base context's theme.
     * Any attributes not explicitly defined in the theme identified by
     * <var>themeResId</var> will retain their original values.
     *
     * @param base       the base context
     * @param themeResId the resource ID of the theme to be applied on top of
     *                   the base context's theme
     */
    public ContextThemeWrapper(Context base, @StyleRes ResourceId themeResId) {
        super(base);
        synchronized (mThemeLock) {
            mThemeResource = themeResId;
        }
    }

    @Override
    public Resources getResources() {
        synchronized (mThemeLock) {
            Resources newResources = super.getResources();
            if (mResources != newResources) {
                mResources = newResources;
                if (mTheme != null) {
                    updateThemeLocked();
                }
            }
        }
        return mResources;
    }

    @Override
    public void setTheme(@Nullable @StyleRes ResourceId resId) {
        synchronized (mThemeLock) {
            mThemeResource = resId;

            if (mTheme == null) {
                return;
            }

            mResources = super.getResources();
            updateThemeLocked();
        }
    }

    @Override
    public Resources.Theme getTheme() {
        synchronized (mThemeLock) {
            Resources newResources = super.getResources();
            if (mResources != newResources) {
                mResources = newResources;
                updateThemeLocked();
            }
        }

        return mTheme;
    }

    private void updateThemeLocked() {
        ThemeKey themeKey = new ThemeKey.Builder()
                .setTo(getBaseContext().getTheme().getKey())
                .append(mThemeResource, true)
                .build();
        mTheme = mResources.newTheme(themeKey);
    }
}
