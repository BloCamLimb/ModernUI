/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.annotation.StyleableRes;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.util.AttributeSet;

/**
 * Interface for obtaining global information about the application environment.
 * This is an abstract class whose implementation is provided by the framework.
 * It allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as launching activities, etc.
 * <p>
 * Note: Unless otherwise specified, all Context objects are thread-safe by
 * simply publishing them safely.
 */
// WIP
public abstract class Context {

    /**
     * Returns a Resources instance for the application's environment.
     * <p>
     * Note: For a Context object, the implementation of this method should always
     * return the same Resources object, even if a resource reload occurs.
     *
     * @return a Resources instance for the application's environment
     */
    public abstract Resources getResources();

    /**
     * Reset the base theme for this context.  Note that this should be called
     * before any views are instantiated in the Context.
     *
     * @param resId The style resource describing the theme.
     */
    public abstract void setTheme(@Nullable @StyleRes ResourceId resId);

    /**
     * Return the Theme object associated with this Context.
     * <p>
     * Note: For a Context object, the implementation of this method should always
     * return the same Theme object, even if a resource reload or theme reset occurs.
     * And the object returned by {@link Resources.Theme#getResources()}
     * should be consistent with {@link #getResources()}. Calling this method is
     * not particularly fast, so it is recommended to cache it in a local variable.
     */
    public abstract Resources.Theme getTheme();

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(String[])
     */
    @NonNull
    public final TypedArray obtainStyledAttributes(@NonNull @StyleableRes String[] attrs) {
        return getTheme().obtainStyledAttributes(attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(ResourceId, String[])
     */
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId resId,
                                                   @NonNull @StyleableRes String[] attrs) {
        return getTheme().obtainStyledAttributes(resId, attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
     */
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                   @NonNull @StyleableRes String[] attrs) {
        return getTheme().obtainStyledAttributes(set, null, null, attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
     */
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                   @Nullable @AttrRes ResourceId defStyleAttr,
                                                   @Nullable @StyleRes ResourceId defStyleRes,
                                                   @NonNull @StyleableRes String[] attrs) {
        return getTheme().obtainStyledAttributes(set, defStyleAttr, defStyleRes, attrs);
    }

    @Nullable
    public abstract Object getSystemService(@NonNull String name);

    /**
     * Use with {@link #getSystemService(String)} to retrieve a
     * {@link icyllis.modernui.view.WindowManager} for accessing the system's window
     * manager.
     *
     * @see #getSystemService(String)
     * @see icyllis.modernui.view.WindowManager
     */
    public static final String WINDOW_SERVICE = "window";
}
