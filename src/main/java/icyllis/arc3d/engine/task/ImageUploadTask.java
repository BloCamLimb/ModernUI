/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.task;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.system.MemoryUtil;

public class ImageUploadTask extends Task {

    public interface UploadCondition {
        //TODO consider close UploadCondition

        boolean PRESERVE = true;
        boolean DISCARD = false;

        boolean needsUpload(ImmediateContext context);

        default boolean onUploadSubmitted() {
            return PRESERVE;
        }
    }

    static class OnceUploadCondition implements UploadCondition {

        public static final UploadCondition INSTANCE = new OnceUploadCondition();

        @Override
        public boolean needsUpload(ImmediateContext context) {
            return true;
        }

        @Override
        public boolean onUploadSubmitted() {
            return DISCARD;
        }
    }

    public static UploadCondition uploadOnce() {
        return OnceUploadCondition.INSTANCE;
    }

    public static class MipLevel {

        public Object mBase;
        public long mAddress;
        public int mRowBytes;

        public MipLevel() {
        }

        public MipLevel(Object base, long address, int rowBytes) {
            mBase = base;
            mAddress = address;
            mRowBytes = rowBytes;
        }

        public MipLevel(Pixmap pixmap) {
            mBase = pixmap.getBase();
            mAddress = pixmap.getAddress();
            mRowBytes = pixmap.getRowBytes();
        }

        public MipLevel(Pixels pixels) {
            mBase = pixels.getBase();
            mAddress = pixels.getAddress();
            mRowBytes = pixels.getRowBytes();
        }
    }

    @RawPtr
    private Buffer mBuffer;
    @SharedPtr
    private ImageViewProxy mImageViewProxy;
    private int mSrcColorType;
    private int mDstColorType;
    private BufferImageCopyData[] mCopyData;
    @Nullable
    private UploadCondition mUploadCondition;

    ImageUploadTask(@RawPtr Buffer buffer,
                    @SharedPtr ImageViewProxy imageViewProxy,
                    int srcColorType,
                    int dstColorType,
                    BufferImageCopyData[] copyData,
                    @Nullable UploadCondition uploadCondition) {
        mBuffer = buffer;
        mImageViewProxy = imageViewProxy;
        mSrcColorType = srcColorType;
        mDstColorType = dstColorType;
        mCopyData = copyData;
        mUploadCondition = uploadCondition;
    }

    @Nullable
    @SharedPtr
    public static ImageUploadTask make(RecordingContext context,
                                       @SharedPtr ImageViewProxy imageViewProxy,
                                       int srcColorType,
                                       int srcAlphaType,
                                       ColorSpace srcColorSpace,
                                       int dstColorType,
                                       int dstAlphaType,
                                       ColorSpace dstColorSpace,
                                       MipLevel[] levels,
                                       Rect2ic dstRect,
                                       UploadCondition condition) {
        assert imageViewProxy != null;
        //TODO take account of Vulkan's optimalBufferCopyOffsetAlignment and
        // optimalBufferCopyRowPitchAlignment

        //TODO note that the dstInfo means for surface, not an actual target to convert,
        // in Vulkan backend we need update the logic

        int mipLevelCount = levels.length;
        // The assumption is either that we have no mipmaps, or that our rect is the entire texture
        assert mipLevelCount == 1 ||
                (dstRect.width() == imageViewProxy.getWidth() &&
                        dstRect.height() == imageViewProxy.getHeight());

        if (dstRect.isEmpty()) {
            imageViewProxy.unref();
            return null;
        }
        for (int i = 0; i < mipLevelCount; i++) {
            // We do not allow any gaps in the mip data
            if (levels[i].mAddress == MemoryUtil.NULL) {
                imageViewProxy.unref();
                return null;
            }
        }

        if (srcColorType == ColorInfo.CT_UNKNOWN ||
                dstColorType == ColorInfo.CT_UNKNOWN) {
            imageViewProxy.unref();
            return null;
        }

        @ColorInfo.ColorType
        int actualColorType = (int) context.getCaps().getSupportedWriteColorType(
                dstColorType,
                imageViewProxy.getDesc(),
                srcColorType
        );
        if (actualColorType == ColorInfo.CT_UNKNOWN) {
            return null;
        }

        int bpp = ColorInfo.bytesPerPixel(actualColorType);

        long[] mipOffsetsAndRowBytes = new long[mipLevelCount * 2 + 2];
        long combinedBufferSize = DataUtils.computeCombinedBufferSize(mipLevelCount,
                bpp,
                dstRect.width(),
                dstRect.height(),
                ColorInfo.COMPRESSION_NONE,
                mipOffsetsAndRowBytes);

        BufferViewInfo bufferInfo = new BufferViewInfo();
        long writer = context.getUploadBufferManager().getUploadPointer(
                combinedBufferSize,
                /*alignment*/ mipOffsetsAndRowBytes[mipLevelCount * 2 + 1],
                bufferInfo
        );
        if (writer == MemoryUtil.NULL) {
            context.getLogger().warn("Failed to get write-mapped buffer for pixel upload of size {}",
                    combinedBufferSize);
            imageViewProxy.unref();
            return null;
        }

        BufferImageCopyData[] copyData = new BufferImageCopyData[mipLevelCount];

        int width = dstRect.width();
        int height = dstRect.height();
        for (int mipLevel = 0; mipLevel < mipLevelCount; mipLevel++) {
            var level = levels[mipLevel];

            int srcRowBytes = level.mRowBytes;

            long mipOffset = mipOffsetsAndRowBytes[mipLevel * 2];
            long dstRowBytes = mipOffsetsAndRowBytes[mipLevel * 2 + 1];

            Object srcBase = level.mBase;
            long srcAddr = level.mAddress;

            ImageInfo srcImageInfo = ImageInfo.make(width, height, srcColorType, srcAlphaType, srcColorSpace);
            ImageInfo dstImageInfo = ImageInfo.make(width, height, actualColorType, dstAlphaType, dstColorSpace);
            boolean res = PixelUtils.convertPixels(
                    srcImageInfo,
                    srcBase,
                    srcAddr,
                    srcRowBytes,
                    dstImageInfo,
                    null,
                    writer + mipOffset,
                    dstRowBytes
            );
            assert res;

            copyData[mipLevel] = new BufferImageCopyData(
                    bufferInfo.mOffset + mipOffset,
                    dstRowBytes,
                    mipLevel,
                    0, 1,
                    dstRect.x(), dstRect.y(), 0,
                    width, height, 1
            );

            width = Math.max(1, width >> 1);
            height = Math.max(1, height >> 1);
        }

        return new ImageUploadTask(
                bufferInfo.mBuffer,
                imageViewProxy, // move
                srcColorType,
                dstColorType,
                copyData,
                condition
        );
    }

    @Override
    protected void deallocate() {
        mImageViewProxy = RefCnt.move(mImageViewProxy);
    }

    @Override
    public int prepare(RecordingContext context) {
        if (!mImageViewProxy.instantiateIfNonLazy(context.getResourceProvider())) {
            return RESULT_FAILURE;
        }
        return RESULT_SUCCESS;
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer commandBuffer) {
        assert mImageViewProxy != null && mImageViewProxy.isInstantiated();

        if (mUploadCondition != null && !mUploadCondition.needsUpload(context)) {
            return RESULT_SUCCESS;
        }

        if (!commandBuffer.copyBufferToImage(mBuffer,
                mImageViewProxy.getImage(),
                mSrcColorType,
                mDstColorType,
                mCopyData)) {
            return RESULT_FAILURE;
        }

        commandBuffer.trackCommandBufferResource(mImageViewProxy.refImage());

        if (mUploadCondition != null && mUploadCondition.onUploadSubmitted() == UploadCondition.DISCARD) {
            return RESULT_DISCARD;
        } else {
            return RESULT_SUCCESS;
        }
    }
}
