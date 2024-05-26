/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.core.*;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The image that is backed by GPU.
 */
public final class Image extends icyllis.arc3d.core.Image {

    RecordingContext mContext;
    @SharedPtr
    ImageProxyView mImageProxyView;

    public Image(@Nonnull RecordingContext rContext,
                 @Nonnull ImageProxy proxy,
                 short swizzle,
                 int origin,
                 int colorType,
                 int alphaType,
                 @Nullable ColorSpace colorSpace) {
        super(ImageInfo.make(proxy.getWidth(), proxy.getHeight(),
                colorType, alphaType, colorSpace));
        mContext = rContext;
        mImageProxyView = new ImageProxyView(RefCnt.create(proxy), origin, swizzle);
    }

    @Override
    protected void deallocate() {
        mImageProxyView.close();
        mImageProxyView = null;
    }

    @Override
    public RecordingContext getContext() {
        return mContext;
    }

    public ImageProxyView getSurfaceProxyView() {
        return mImageProxyView;
    }

    @Override
    public boolean isValid(@Nullable RecordingContext context) {
        if (mContext.isDiscarded()) {
            return false;
        }
        if (context != null) {
            return !context.isDiscarded() && mContext.matches(context);
        }
        return true;
    }

    @Override
    public boolean isGpuBacked() {
        return true;
    }

    @Override
    public long getGpuMemorySize() {
        return mImageProxyView.getProxy().getMemorySize();
    }
}
