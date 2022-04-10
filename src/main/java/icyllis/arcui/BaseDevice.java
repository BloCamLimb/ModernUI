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

package icyllis.arcui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class for drawing devices.
 */
public abstract class BaseDevice extends MatrixProvider {

    protected static final int
            CLIP_TYPE_EMPTY = 0,
            CLIP_TYPE_RECT = 1,
            CLIP_TYPE_COMPLEX = 2;

    protected final Rect mBounds = new Rect();

    final ImageInfo mInfo;
    // mDeviceToGlobal and mGlobalToDevice are inverses of each other; there are never that many
    // Devices, so pay the memory cost to avoid recalculating the inverse.
    final Matrix3 mDeviceToGlobal = Matrix3.identity();
    final Matrix3 mGlobalToDevice = Matrix3.identity();

    MarkerStack mMarkerStack;

    public BaseDevice(ImageInfo info) {
        mInfo = info;
        mBounds.set(0, 0, info.width(), info.height());
    }

    /**
     * Return ImageInfo for this device. If the canvas is not backed by GPU,
     * then the info's ColorType will be {@link ImageInfo#COLOR_UNKNOWN}.
     */
    @Nonnull
    public final ImageInfo imageInfo() {
        return mInfo;
    }

    public final int width() {
        return mInfo.width();
    }

    public final int height() {
        return mInfo.height();
    }

    public final void getBounds(Rect bounds) {
        bounds.set(mBounds);
    }

    /**
     * Return the bounds of the device in the coordinate space of the root
     * canvas. The root device will have its top-left at 0,0, but other devices
     * such as those associated with saveLayer may have a non-zero origin.
     */
    public final void getGlobalBounds(Rect bounds) {
        if (mDeviceToGlobal.isIdentity()) {
            bounds.set(mBounds);
        } else {
            mDeviceToGlobal.mapRectOut(mBounds, bounds);
        }
    }

    /**
     * Returns the bounding box of the current clip, in this device's
     * coordinate space. No pixels outside these bounds will be touched by
     * draws unless the clip is further modified (at which point this will
     * return the updated bounds).
     */
    public final void getClipBounds(Rect bounds) {
        bounds.set(getClipBounds());
    }

    /**
     * Return the device's coordinate space transform: this maps from the device's coordinate space
     * into the global canvas' space (or root device space). This includes the translation
     * necessary to account for the device's origin.
     */
    public final Matrix3 deviceToGlobal() {
        return mDeviceToGlobal;
    }

    /**
     * Return the inverse of getDeviceToGlobal(), mapping from the global canvas' space (or root
     * device space) into this device's coordinate space.
     */
    public final Matrix3 globalToDevice() {
        return mGlobalToDevice;
    }

    /**
     * Returns true when this device's pixel grid is axis aligned with the global coordinate space,
     * and any relative translation between the two spaces is in integer pixel units.
     */
    public final boolean isPixelAlignedToGlobal() {
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
    public final void getRelativeTransform(final BaseDevice device, Matrix3 out) {
        // To get the transform from this space to the other device's, transform from our space to
        // global and then from global to the other device.
        out.set(mDeviceToGlobal);
        out.postMul(device.mGlobalToDevice);
    }

    @Override
    public final boolean getLocalToMarker(int id, Matrix4 localToMarker) {
        // The marker stack stores CTM snapshots, which are "marker to global" matrices.
        // We ask for the (cached) inverse, which is a "global to marker" matrix.
        Matrix4 globalToMarker = null;
        // ID 0 is special, and refers to the CTM (local-to-global)
        if (mMarkerStack != null && (id == 0 || (globalToMarker = mMarkerStack.findMarkerInverse(id)) != null)) {
            // globalToMarker will still be the identity if id is zero
            if (globalToMarker == null) {
                localToMarker.setIdentity();
            } else {
                localToMarker.set(globalToMarker);
            }
            localToMarker.preMul(mDeviceToGlobal);
            localToMarker.preMul(mLocalToDevice);
            return true;
        }
        return false;
    }

    public final MarkerStack markerStack() {
        return mMarkerStack;
    }

    public final void setMarkerStack(MarkerStack ms) {
        mMarkerStack = ms;
    }

    public final void save() {
        onSave();
    }

    public final void restore(Matrix4 globalTransform) {
        onRestore();
        setGlobalTransform(globalTransform);
    }

    public final void restoreLocal(Matrix4 localToDevice) {
        onRestore();
        setLocalToDevice(localToDevice);
    }

    public void clipRect(RectF rect, int clipOp, boolean doAA) {
    }

    public void replaceClip(Rect rect) {
    }

    public abstract boolean clipIsAA();

    public abstract boolean clipIsWideOpen();

    public final void setGlobalTransform(@Nullable Matrix4 globalTransform) {
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

    public final void setLocalToDevice(@Nullable Matrix4 localToDevice) {
        if (localToDevice == null) {
            mLocalToDevice.setIdentity();
        } else {
            mLocalToDevice.set(localToDevice);
        }
    }

    /**
     * Configure the device's coordinate spaces, specifying both how its device image maps back to
     * the global space (via 'deviceToGlobal') and the initial transform of the device (via
     * 'localToDevice', i.e. what geometry drawn into this device will be transformed with).
     * <p>
     * (bufferOriginX, bufferOriginY) defines where the (0,0) pixel the device's backing buffer
     * is anchored in the device space. The final device-to-global matrix stored by the SkDevice
     * will include a pre-translation by T(deviceOriginX, deviceOriginY), and the final
     * local-to-device matrix will have a post-translation of T(-deviceOriginX, -deviceOriginY).
     */
    final void setCoordinateSystem(@Nullable Matrix3 deviceToGlobal, @Nullable Matrix4 localToDevice,
                                   int bufferOriginX, int bufferOriginY) {
        if (deviceToGlobal == null) {
            mDeviceToGlobal.setIdentity();
            mGlobalToDevice.setIdentity();
        } else {
            mDeviceToGlobal.set(deviceToGlobal);
            mDeviceToGlobal.normalizePerspective();
            if (!mDeviceToGlobal.invert(mGlobalToDevice)) {
                throw new IllegalStateException();
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
    final void setOrigin(@Nullable Matrix4 globalTransform, int x, int y) {
        setCoordinateSystem(null, globalTransform, x, y);
    }

    protected void onSave() {
    }

    protected void onRestore() {
    }

    protected abstract int getClipType();

    protected abstract Rect getClipBounds();

    /**
     * These are called inside the per-device-layer loop for each draw call.
     * When these are called, we have already applied any saveLayer operations,
     * and are handling any looping from the paint.
     */
    protected abstract void drawPaint(Paint paint);

    /**
     * Create a new device based on CreateInfo. If the paint is not null, then it represents a
     * preview of how the new device will be composed with its creator device (this).
     * <p>
     * The subclass may be handed this device in drawDevice(), so it must always return
     * a device that it knows how to draw, and that it knows how to identify if it is not of the
     * same subclass (since drawDevice is passed a BaseDevice). If the subclass cannot fulfill
     * that contract it should return null.
     */
    @Nullable
    protected BaseDevice onCreateDevice(ImageInfo info, @Nullable Paint paint) {
        return null;
    }
}
