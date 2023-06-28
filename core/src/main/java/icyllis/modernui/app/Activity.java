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

package icyllis.modernui.app;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.WindowManager;
import icyllis.modernui.widget.ToastManager;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public abstract class Activity extends Context {

    private volatile ToastManager mToastManager;

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
}
