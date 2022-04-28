/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

import javax.annotation.Nonnull;
import java.lang.ref.Cleaner;

/**
 * Represents the application-controlled 3D API server, holding a reference
 * to DirectContext. It is responsible for creating / deleting 3D API objects,
 * controlling binding status, uploading and downloading data, transferring
 * 3D API draw commands, etc.
 */
public abstract class Server {

    private static final Cleaner sCleaner = Cleaner.create();

    private final DirectContext mContext;

    public Server(DirectContext context) {
        mContext = context;
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. It will be registered with the global cleaner shared across Arc UI.
     * The action object should never hold any reference to the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance for explicit cleaning
     */
    @Nonnull
    public static Cleaner.Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return sCleaner.register(target, action);
    }

    public DirectContext getContext() {
        return mContext;
    }

    /**
     * Gets the capabilities of the draw target.
     */
    public abstract Caps getCaps();
}
