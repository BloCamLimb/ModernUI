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

package icyllis.modernui.core;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.resources.Resources;

/**
 * Interface for obtaining global information about the application environment.
 * This is an abstract class whose implementation is provided by the framework.
 * It allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as launching activities, etc.
 */
// WIP
public abstract class Context {

    public abstract Resources getResources();

    public abstract Resources.Theme getTheme();

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
