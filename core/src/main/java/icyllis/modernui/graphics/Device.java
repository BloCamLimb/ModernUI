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

import icyllis.modernui.math.Matrix3;
import icyllis.modernui.math.Matrix4;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The drawing device.
 */
public final class Device extends MatrixProvider {

    final ImageInfo mInfo;

    final Matrix3 mDeviceToGlobal = Matrix3.identity();
    final Matrix3 mGlobalToDevice = Matrix3.identity();

    public Device(ImageInfo info) {
        mInfo = info;
    }

    /**
     * Return ImageInfo for this device. If the canvas is not backed by GPU,
     * then the info's ColorType will be {@link ImageInfo#COLOR_UNKNOWN}.
     */
    @Nonnull
    public ImageInfo imageInfo() {
        return mInfo;
    }

    public int width() {
        return mInfo.width();
    }

    public int height() {
        return mInfo.height();
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
    void setCoordinateSpace(@Nullable Matrix3 deviceToGlobal, @Nullable Matrix4 localToDevice,
                            int bufferOriginX, int bufferOriginY) {
        if (deviceToGlobal == null) {
            mDeviceToGlobal.setIdentity();
            mGlobalToDevice.setIdentity();
        } else {
            mDeviceToGlobal.set(deviceToGlobal);
            mDeviceToGlobal.normalizePerspective();
            if (!mDeviceToGlobal.invert(mGlobalToDevice)) {
                throw new IllegalArgumentException();
            }
        }
        if (localToDevice == null) {
            mLocalToDevice.setIdentity();
        } else {
            mLocalToDevice.set(localToDevice);
            mLocalToDevice.normalizePerspective();
        }
        if ((bufferOriginX | bufferOriginY) != 0) {
            mDeviceToGlobal.preTranslate(bufferOriginX, bufferOriginY);
            mGlobalToDevice.postTranslate(-bufferOriginX, -bufferOriginY);
            mLocalToDevice.postTranslate(-bufferOriginX, -bufferOriginY);
        }
    }

    /**
     * Convenience to configure the device to be axis-aligned with the root canvas, but with a
     * unique origin.
     */
    void setOrigin(@Nullable Matrix4 globalTransform, int x, int y) {
        setCoordinateSpace(null, globalTransform, x, y);
    }

    public void setGlobalTransform(@Nullable Matrix4 globalTransform) {
        if (globalTransform == null) {
            mLocalToDevice.setIdentity();
        } else {
            mLocalToDevice.set(globalTransform);
            mLocalToDevice.normalizePerspective();
        }
        if (!mGlobalToDevice.isIdentity()) {
            // Map from the global CTM state to this device's coordinate system.
            mLocalToDevice.postMul(mGlobalToDevice);
        }
    }

    /**
     * Returns true when this device's pixel grid is axis aligned with the global coordinate space,
     * and any relative translation between the two spaces is in integer pixel units.
     */
    public boolean isPixelAlignedToGlobal() {
        float x = mDeviceToGlobal.getTranslateX();
        float y = mDeviceToGlobal.getTranslateY();
        return x == Math.round(x) && y == Math.round(y) && mDeviceToGlobal.isTranslate();
    }

    /**
     * Get the transformation from this device's coordinate system to the provided device space.
     * This transform can be used to draw this device into the provided device, such that once
     * that device is drawn to the root device, the net effect will be that this device's contents
     * have been transformed by the global transform.
     */
    public void getRelativeTransform(final Device dstDevice, Matrix3 mat) {
        // To get the transform from this space to the other device's, transform from our space to
        // global and then from global to the other device.
        mat.set(mDeviceToGlobal);
        mat.postMul(dstDevice.mGlobalToDevice);
    }

    @Override
    public boolean getLocalToMarker(int id, Matrix4 localToMarker) {
        return false;
    }
}
