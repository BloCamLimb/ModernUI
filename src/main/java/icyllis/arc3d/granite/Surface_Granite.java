/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.RecordingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The surface that is backed by GPU.
 */
public final class Surface_Granite extends icyllis.arc3d.core.Surface {

    @SharedPtr
    private Device_Granite mDevice;

    public Surface_Granite(@SharedPtr Device_Granite device) {
        super(device.width(), device.height());
        mDevice = device;
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mDevice = RefCnt.move(mDevice);
    }

    public void flush() {
        mDevice.flushPendingWork();
    }

    @Nonnull
    @Override
    public ImageInfo getImageInfo() {
        return mDevice.imageInfo();
    }

    @Override
    protected Canvas onNewCanvas() {
        return new Canvas(RefCnt.create(mDevice));
    }

    @Nullable
    @Override
    protected Image onNewImageSnapshot(@Nullable Rect2ic subset) {
        //TODO
        return null;
    }

    @Override
    protected boolean onCopyOnWrite(int changeMode) {
        // onNewImageSnapshot() always copy, no-op here
        return true;
    }

    @Override
    protected RecordingContext onGetRecordingContext() {
        return mDevice.getRecordingContext();
    }
}
