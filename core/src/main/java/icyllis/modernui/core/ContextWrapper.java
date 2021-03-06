/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public abstract class ContextWrapper extends Context {

    private Context mBase;

    public ContextWrapper() {

    }

    public ContextWrapper(@Nullable Context base) {
        mBase = base;
    }

    /**
     * Set the base context for this ContextWrapper.  All calls will then be
     * delegated to the base context.
     *
     * @param base The new base context for this wrapper.
     * @throws IllegalStateException a base context has already been set
     */
    protected void attachBaseContext(@Nonnull Context base) {
        if (mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBase = base;
    }

    /**
     * @return the base context as set by the constructor or setBaseContext
     */
    public Context getBaseContext() {
        return mBase;
    }

    @Override
    public ReadableByteChannel getResource(@Nonnull Path path) throws IOException {
        return mBase.getResource(path);
    }

    @Nullable
    @Override
    public Image getImage(@Nonnull Path path, boolean antiAliasing) {
        return mBase.getImage(path, antiAliasing);
    }
}
