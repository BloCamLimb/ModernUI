/*
 * ModernUI.
 * Copyright (C) 2023-2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.util.AttributeSet;
import org.jetbrains.annotations.ApiStatus;

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
     *
     * @return a Resources instance for the application's environment
     *
     * @deprecated Resources object can be changed during calls, use {@link #getCurrentResources()} instead.
     */
    @Deprecated
    public abstract Resources getResources();

    /**
     * Returns current Resources instance for the application's environment.
     * <p>
     * This method is atomic. The implementation of this method can return a different
     * Resources object on each call, which can happen during resource reloads or
     * configuration changes. If you need to obtain a consistent batch/nested set of
     * resources, you should remain on the same Resources object; therefore, avoid
     * calling this method again during this period.
     *
     * @return current Resources instance for the application's environment
     */
    public Resources getCurrentResources() {
        return getResources();
    }

    public ClassLoader getClassLoader() {
        return Context.class.getClassLoader();
    }

    /**
     * Reset the base theme for this context.  Note that this should be called
     * before any views are instantiated in the Context.
     *
     * @param resId The style resource describing the theme.
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself
     */
    @Deprecated
    public abstract void setTheme(@Nullable @StyleRes ResourceId resId);

    /**
     * Return the Theme object associated with this Context.
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself
     */
    @Deprecated
    public abstract Resources.Theme getTheme();

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(String[])
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself
     */
    @Deprecated(forRemoval = true)
    @NonNull
    public final TypedArray obtainStyledAttributes(@NonNull String[] attrs) {
        return getTheme().obtainStyledAttributes(attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(ResourceId, String[])
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself
     */
    @Deprecated(forRemoval = true)
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable @StyleRes ResourceId resId,
                                                   @NonNull String[] attrs) {
        return getTheme().obtainStyledAttributes(resId, attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself
     */
    @Deprecated(forRemoval = true)
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                   @NonNull String[] attrs) {
        return getTheme().obtainStyledAttributes(set, (String) null, null, attrs);
    }

    /**
     * Retrieve styled attribute information in this Context's theme.  See
     * {@link Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])}
     * for more information.
     *
     * @see Resources.Theme#obtainStyledAttributes(AttributeSet, ResourceId, ResourceId, String[])
     *
     * @deprecated theme will no longer be part of Context object, you should create it yourself;
     *          namespace and type are not used in attribute name anymore, this will be removed in 4.0
     */
    @Deprecated(forRemoval = true)
    @NonNull
    public final TypedArray obtainStyledAttributes(@Nullable AttributeSet set,
                                                   @Nullable @AttrRes ResourceId defStyleAttr,
                                                   @Nullable @StyleRes ResourceId defStyleRes,
                                                   @NonNull String[] attrs) {
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

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final String TOAST_SERVICE = "toast";
}
