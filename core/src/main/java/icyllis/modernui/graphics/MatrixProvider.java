/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.math.Matrix4;

import javax.annotation.Nonnull;

public class MatrixProvider {

    final Matrix4 mLocalToDevice;

    /**
     * Create a matrix provider using an identity matrix.
     */
    public MatrixProvider() {
        this(Matrix4.identity());
    }

    /**
     * Create a matrix provider using the given matrix.
     *
     * @param localToDevice the backing matrix
     */
    public MatrixProvider(@Nonnull Matrix4 localToDevice) {
        mLocalToDevice = localToDevice;
    }

    /**
     * @return the backing matrix
     */
    @Nonnull
    public final Matrix4 localToDevice() {
        return mLocalToDevice;
    }
}
