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

/**
 * The drawing device.
 */
public final class Device extends MatrixProvider {

    // image info
    final int mWidth;
    final int mHeight;
    final int mColorInfo;

    // fDeviceToGlobal and fGlobalToDevice are inverses of each other; there are never that many
    // SkDevices, so pay the memory cost to avoid recalculating the inverse.
    Matrix4 fDeviceToGlobal;
    Matrix4 fGlobalToDevice;

    public Device(int width, int height, int colorInfo) {
        mWidth = width;
        mHeight = height;
        mColorInfo = colorInfo;
    }

    public int width() {
        return mWidth;
    }

    public int height() {
        return mHeight;
    }

    public int colorInfo() {
        return mColorInfo;
    }

    /**
     * Configure the device's coordinate spaces, specifying both how its device image maps back to
     * the global space (via 'deviceToGlobal') and the initial CTM of the device (via
     * 'localToDevice', i.e. what geometry drawn into this device will be transformed with).
     * <p>
     * (bufferOriginX, bufferOriginY) defines where the (0,0) pixel the device's backing buffer
     * is anchored in the device space. The final device-to-global matrix stored by the SkDevice
     * will include a pre-translation by T(deviceOriginX, deviceOriginY), and the final
     * local-to-device matrix will have a post-translation of T(-deviceOriginX, -deviceOriginY).
     */
    void setDeviceCoordinateSystem(final Matrix4 deviceToGlobal, final Matrix4 localToDevice,
                                   int bufferOriginX, int bufferOriginY) {

    }

    @Override
    public boolean getLocalToMarker(int id, Matrix4 localToMarker) {
        return false;
    }
}
