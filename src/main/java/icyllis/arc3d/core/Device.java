/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import icyllis.arc3d.engine.RecordingContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class for drawing devices.
 */
public abstract class Device extends RefCnt {

    private final ImageInfo mInfo;
    private final Rect2i mBounds = new Rect2i();

    final Matrix4 mLocalToDevice = new Matrix4();
    final Matrix mLocalToDevice33 = new Matrix();

    // mDeviceToGlobal and mGlobalToDevice are inverses of each other
    final Matrix4 mDeviceToGlobal = new Matrix4();
    final Matrix4 mGlobalToDevice = new Matrix4();

    public Device(ImageInfo info) {
        mInfo = info;
        mBounds.set(0, 0, info.width(), info.height());
    }

    /**
     * Internal resize for optimization purposes.
     */
    void resize(int width, int height) {
        mInfo.resize(width, height);
        mBounds.set(0, 0, width, height);
    }

    @Override
    protected void deallocate() {
    }

    /**
     * Return image info for this device.
     */
    @NonNull
    public final ImageInfo getImageInfo() {
        return mInfo;
    }

    public final int getWidth() {
        return mInfo.width();
    }

    public final int getHeight() {
        return mInfo.height();
    }

    /**
     * @return read-only bounds
     */
    public final Rect2ic getBounds() {
        return mBounds;
    }

    /**
     * Return the bounds of the device in the coordinate space of this device.
     */
    public final void getBounds(@NonNull Rect2i bounds) {
        bounds.set(mBounds);
    }

    /**
     * Return the bounds of the device in the coordinate space of the root
     * canvas. The root device will have its top-left at 0,0, but other devices
     * such as those associated with saveLayer may have a non-zero origin.
     */
    public final void getGlobalBounds(@NonNull Rect2i bounds) {
        mDeviceToGlobal.mapRectOut(mBounds, bounds);
    }

    /**
     * Returns the transformation that maps from the local space to the device's coordinate space.
     */
    @NonNull
    public final Matrix4c getLocalToDevice() {
        return mLocalToDevice;
    }

    /**
     * Returns the transformation that maps from the local space to the device's coordinate space.
     */
    @NonNull
    public final Matrixc getLocalToDevice33() {
        return mLocalToDevice33;
    }

    /**
     * Return the device's coordinate space transform: this maps from the device's coordinate space
     * into the global canvas' space (or root device space). This includes the translation
     * necessary to account for the device's origin.
     */
    @NonNull
    public final Matrix4c getDeviceToGlobal() {
        return mDeviceToGlobal;
    }

    /**
     * Return the inverse of getDeviceToGlobal(), mapping from the global canvas' space (or root
     * device space) into this device's coordinate space.
     */
    @NonNull
    public final Matrix4c getGlobalToDevice() {
        return mGlobalToDevice;
    }

    /**
     * Returns true when this device's pixel grid is axis aligned with the global coordinate space,
     * and any relative translation between the two spaces is in integer pixel units.
     */
    public final boolean isPixelAlignedToGlobal() {
        Matrix4 mat = mDeviceToGlobal;
        return mat.m11 == 1 && mat.m12 == 0 && mat.m13 == 0 && mat.m14 == 0 &&
                mat.m21 == 0 && mat.m22 == 1 && mat.m23 == 0 && mat.m24 == 0 &&
                mat.m31 == 0 && mat.m32 == 0 && mat.m33 == 1 && mat.m34 == 0 &&
                mat.m41 == Math.floor(mat.m41) && mat.m42 == Math.floor(mat.m42) && mat.m43 == 0 && mat.m44 == 1;
    }

    /**
     * Get the transformation from this device's coordinate system to the provided device space.
     * This transform can be used to draw this device into the provided device, such that once
     * that device is drawn to the root device, the net effect will be that this device's contents
     * have been transformed by the global transform.
     */
    public final void getRelativeTransform(@NonNull Device device, @NonNull Matrix4 dest) {
        // To get the transform from this space to the other device's, transform from our space to
        // global and then from global to the other device.
        dest.set(mDeviceToGlobal);
        dest.postConcat(device.mGlobalToDevice);
    }

    public final void setGlobalCTM(@NonNull Matrix4c ctm) {
        mLocalToDevice.set(ctm);
        mLocalToDevice.normalizePerspective();
        // Map from the global CTM state to this device's coordinate system.
        mLocalToDevice.postConcat(mGlobalToDevice);
        mLocalToDevice.toMatrix(mLocalToDevice33);
    }

    public final void setLocalToDevice(@NonNull Matrix4c localToDevice) {
        mLocalToDevice.set(localToDevice);
        mLocalToDevice.toMatrix(mLocalToDevice33);
    }

    @Nullable
    public RecordingContext getRecordingContext() {
        return null;
    }

    /**
     * Configure the device's coordinate spaces, specifying both how its device image maps back to
     * the global space (via 'deviceToGlobal') and the initial transform of the device (via
     * 'localToDevice', i.e. what geometry drawn into this device will be transformed with).
     * <p>
     * (bufferOriginX, bufferOriginY) defines where the (0,0) pixel the device's backing buffer
     * is anchored in the device space. The final device-to-global matrix stored by the Device
     * will include a pre-translation by T(deviceOriginX, deviceOriginY), and the final
     * local-to-device matrix will have a post-translation of T(-deviceOriginX, -deviceOriginY).
     */
    protected final void setDeviceCoordinateSystem(@Nullable Matrix4c deviceToGlobal,
                                                   @Nullable Matrix4c globalToDevice,
                                                   @Nullable Matrix4c localToDevice,
                                                   int bufferOriginX,
                                                   int bufferOriginY) {
        if (deviceToGlobal == null) {
            mDeviceToGlobal.setIdentity();
            mGlobalToDevice.setIdentity();
        } else {
            assert (globalToDevice != null);
            mDeviceToGlobal.set(deviceToGlobal);
            mDeviceToGlobal.normalizePerspective();
            mGlobalToDevice.set(globalToDevice);
            mGlobalToDevice.normalizePerspective();
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
        mLocalToDevice.toMatrix(mLocalToDevice33);
    }

    /**
     * Convenience to configure the device to be axis-aligned with the root canvas, but with a
     * unique origin.
     */
    final void setOrigin(@Nullable Matrix4 globalTransform, int x, int y) {
        setDeviceCoordinateSystem(null, null, globalTransform, x, y);
    }

    public abstract void pushClipStack();

    public abstract void popClipStack();

    /**
     * Returns the bounding box of the current clip, in this device's
     * coordinate space. No pixels outside these bounds will be touched by
     * draws unless the clip is further modified (at which point this will
     * return the updated bounds).
     */
    public abstract void getClipBounds(@NonNull Rect2i bounds);

    protected abstract Rect2ic getClipBounds();

    public abstract void clipRect(Rect2fc rect, int clipOp, boolean doAA);

    public abstract boolean isClipAA();

    public abstract boolean isClipEmpty();

    public abstract boolean isClipRect();

    public abstract boolean isClipWideOpen();

    public abstract void drawPaint(Paint paint);

    public abstract void drawPoints(int mode, float[] pts, int offset,
                                    int count, Paint paint);

    public abstract void drawLine(float x0, float y0, float x1, float y1,
                                  @Paint.Cap int cap, float width, Paint paint);

    public abstract void drawRect(Rect2fc r,
                                  Paint paint);

    public abstract void drawRRect(RRect rr, Paint paint);

    public abstract void drawCircle(float cx, float cy, float radius, Paint paint);

    public abstract void drawArc(float cx, float cy, float radius, float startAngle,
                                 float sweepAngle, @Paint.Cap int cap, float width, Paint paint);

    public abstract void drawPie(float cx, float cy, float radius, float startAngle,
                                 float sweepAngle, Paint paint);

    public abstract void drawChord(float cx, float cy, float radius, float startAngle,
                                   float sweepAngle, Paint paint);

    public abstract void drawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst,
                                       SamplingOptions sampling, Paint paint,
                                       int constraint);

    public final void drawGlyphRunList(Canvas canvas,
                                       GlyphRunList glyphRunList,
                                       Paint paint) {
        if (!getLocalToDevice33().isFinite()) {
            return;
        }

        onDrawGlyphRunList(canvas, glyphRunList, paint);
    }

    protected abstract void onDrawGlyphRunList(Canvas canvas,
                                               GlyphRunList glyphRunList,
                                               Paint paint);

    public abstract void drawVertices(Vertices vertices, @SharedPtr Blender blender,
                                      Paint paint);

    public void drawEdgeAAQuad(Rect2fc r, float[] clip, int flags, Paint paint) {
    }

    @Nullable
    protected Surface makeSurface(ImageInfo info) {
        return null;
    }

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
    protected Device createDevice(ImageInfo info, @Nullable Paint paint) {
        return null;
    }
}
