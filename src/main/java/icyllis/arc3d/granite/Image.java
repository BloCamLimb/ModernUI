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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The image that is backed by GPU.
 */
public final class Image extends icyllis.arc3d.core.Image {

    Context mContext;
    @SharedPtr
    ImageViewProxy mImageViewProxy;

    public Image(@Nonnull Context rContext,
                 @Nonnull ImageViewProxy proxy,
                 int colorType,
                 int alphaType,
                 @Nullable ColorSpace colorSpace) {
        super(ImageInfo.make(proxy.getWidth(), proxy.getHeight(),
                colorType, alphaType, colorSpace));
        mContext = rContext;
        mImageViewProxy = RefCnt.create(proxy);
    }

    @Override
    protected void deallocate() {
        mImageViewProxy.unref();
        mImageViewProxy = null;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    public ImageViewProxy getImageViewProxy() {
        return mImageViewProxy;
    }

    @Override
    public boolean isValid(@Nullable Context context) {
        /*if (mContext.isDiscarded()) {
            return false;
        }
        if (context != null) {
            return !context.isDiscarded() && mContext.matches(context);
        }*/
        //TODO
        return true;
    }

    @Override
    public boolean isGpuBacked() {
        return true;
    }

    @Override
    public long getGpuMemorySize() {
        return mImageViewProxy.getMemorySize();
    }
}
