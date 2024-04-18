/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.image;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Image_Texture extends Image {

    RecordingContext mContext;
    @SharedPtr
    SurfaceProxyView mSurfaceProxyView;

    public Image_Texture(@Nonnull RecordingContext rContext,
                         @Nonnull ImageProxy proxy,
                         short swizzle,
                         int origin,
                         int colorType,
                         int alphaType,
                         @Nullable ColorSpace colorSpace) {
        super(ImageInfo.make(proxy.getBackingWidth(), proxy.getBackingHeight(),
                colorType, alphaType, colorSpace));
        mContext = rContext;
        mSurfaceProxyView = new SurfaceProxyView(RefCnt.create(proxy), origin, swizzle);
    }

    @Override
    protected void deallocate() {
        mSurfaceProxyView.close();
        mSurfaceProxyView = null;
    }

    @Override
    public RecordingContext getContext() {
        return mContext;
    }

    public SurfaceProxyView getSurfaceProxyView() {
        return mSurfaceProxyView;
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
    public boolean isTextureBacked() {
        return true;
    }

    @Override
    public long getTextureMemorySize() {
        return mSurfaceProxyView.getProxy().getMemorySize();
    }
}
