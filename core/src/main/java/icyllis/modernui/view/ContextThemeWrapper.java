/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.ContextWrapper;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;

/**
 * A context wrapper that allows you to modify or replace the theme of the
 * wrapped context.
 */
public class ContextThemeWrapper extends ContextWrapper {
    private final Object mThemeLock = new Object();
    private ResourceId mThemeResource;
    private volatile Resources.Theme mTheme;
    private volatile Resources mResources;

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

    /**
     * Creates a new context wrapper with the specified theme.
     * <p>
     * Unlike {@link #ContextThemeWrapper(Context, ResourceId)}, the theme passed to
     * this constructor will completely replace the base context's theme and the
     * Resources object.
     *
     * @param base  the base context
     * @param theme the theme against which resources should be inflated
     */
    public ContextThemeWrapper(Context base, Resources.Theme theme) {
        super(base);
        mTheme = theme;
        if (theme != null) {
            mResources = theme.getResources();
        }
    }

    @Override
    public Resources getResources() {
        return getResourcesInternal();
    }

    private Resources getResourcesInternal() {
        final Resources resources = mResources;
        if (resources != null) {
            return resources;
        }
        synchronized (mThemeLock) {
            if (mResources == null) {
                mResources = super.getResources();
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

            final Resources.Theme baseTheme = getBaseContext().getTheme();
            mTheme.setTo(baseTheme);
            onApplyThemeResource(mTheme, mThemeResource, false);
        }
    }

    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = mTheme;
        if (theme != null) {
            return theme;
        }

        synchronized (mThemeLock) {
            if (mTheme == null) {
                if (mResources == null) {
                    mResources = super.getResources();
                }
                mTheme = mResources.newTheme();
                final Resources.Theme baseTheme = getBaseContext().getTheme();
                mTheme.setTo(baseTheme);
                onApplyThemeResource(mTheme, mThemeResource, true);
            }
        }

        return mTheme;
    }

    /**
     * Called by {@link #setTheme} and {@link #getTheme} to apply a theme
     * resource to the current Theme object. May be overridden to change the
     * default (simple) behavior. This method will not be called in multiple
     * threads simultaneously.
     *
     * @param theme the theme being modified
     * @param resId the style resource being applied to <var>theme</var>
     * @param first {@code true} if this is the first time a style is being
     *              applied to <var>theme</var>
     */
    protected void onApplyThemeResource(@NonNull Resources.Theme theme,
                                        @Nullable @StyleRes ResourceId resId, boolean first) {
        if (resId != null) {
            theme.applyStyle(resId, true);
        }
    }
}
