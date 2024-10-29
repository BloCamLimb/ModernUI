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

package icyllis.arc3d.vulkan;

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.ShaderCaps;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.engine.trash.PipelineKey_old;
import icyllis.arc3d.opengl.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK11.*;

public class VulkanCaps extends Caps {

    /**
     * Vulkan image format table.
     *
     * @see VKUtil#vkFormatToIndex(int)
     */
    final FormatInfo[] mFormatTable =
            new FormatInfo[VKUtil.LAST_COLOR_FORMAT_INDEX + 1];

    // may contain VK_FORMAT_UNDEFINED(0) values that representing unsupported
    private final int[] mColorTypeToBackendFormat =
            new int[ColorInfo.CT_COUNT];

    public VulkanCaps(ContextOptions options,
                      VkPhysicalDevice physDev,
                      int physicalDeviceVersion,
                      VkPhysicalDeviceFeatures2 deviceFeatures2,
                      VKCapabilitiesInstance capabilitiesInstance,
                      VKCapabilitiesDevice capabilitiesDevice) {
        super(options);

        mDepthClipNegativeOneToOne = false;

        ShaderCaps shaderCaps = mShaderCaps;
        shaderCaps.mTargetApi = TargetApi.VULKAN_1_0;
        shaderCaps.mGLSLVersion = GLSLVersion.GLSL_450;

        try (var stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties physProps = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(physDev, physProps);
            VkPhysicalDeviceLimits limits = physProps.limits();

            if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 3, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_6;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 2, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_5;
            } else if (Integer.compareUnsigned(physicalDeviceVersion,
                    VK_MAKE_VERSION(1, 1, 0)) >= 0) {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_3;
            } else {
                shaderCaps.mSPIRVVersion = SPIRVVersion.SPIRV_1_0;
            }

            mMaxTextureSize = (int) Math.min(
                    Integer.toUnsignedLong(limits.maxImageDimension2D()), Integer.MAX_VALUE);

            initFormatTable(options, physDev, physProps, stack);
        }
    }

    void initFormatTable(ContextOptions options,
                         VkPhysicalDevice physDev,
                         VkPhysicalDeviceProperties physProps,
                         MemoryStack stack) {
        for (int i = 1; i < mFormatTable.length; i++) {
            mFormatTable[i] = new FormatInfo();
        }

        // Format: VK_FORMAT_R8G8B8A8_UNORM
        {
            final int format = VK_FORMAT_R8G8B8A8_UNORM;
            FormatInfo info = getFormatInfo(format);
            info.init(options, physDev, physProps, format, stack);
            if (info.isTexturable(VK_IMAGE_TILING_OPTIMAL)) {
                info.mColorTypeInfos = new ColorTypeInfo[2];
                // Format: VK_FORMAT_R8G8B8A8_UNORM, Surface: kRGBA_8888
                {
                    ColorTypeInfo ctInfo = info.mColorTypeInfos[0] = new ColorTypeInfo();
                    ctInfo.mColorType = ColorInfo.CT_RGBA_8888;
                    ctInfo.mTransferColorType = ColorInfo.CT_RGBA_8888;
                    ctInfo.mFlags = ColorTypeInfo.kUploadData_Flag | ColorTypeInfo.kRenderable_Flag;
                }
                // Format: VK_FORMAT_R8G8B8A8_UNORM, Surface: kRGB_888x
                {
                    ColorTypeInfo ctInfo = info.mColorTypeInfos[1] = new ColorTypeInfo();
                    ctInfo.mColorType = ColorInfo.CT_RGB_888x;
                    ctInfo.mTransferColorType = ColorInfo.CT_RGB_888x;
                    ctInfo.mFlags = ColorTypeInfo.kUploadData_Flag;
                    ctInfo.mReadSwizzle = Swizzle.RGB1;
                }
            }
        }

        // Reserved for undefined
        mFormatTable[0] = new FormatInfo();

        setColorTypeFormat(ColorInfo.CT_RGBA_8888, VK_FORMAT_R8G8B8A8_UNORM);
    }

    FormatInfo getFormatInfo(@NativeType("VkFormat") int format) {
        return mFormatTable[VKUtil.vkFormatToIndex(format)];
    }

    // Map ColorType to VkFormat with fallback list
    private void setColorTypeFormat(int colorType, int... formats) {
        for (int format : formats) {
            assert VKUtil.vkFormatIsSupported(format);
            var info = getFormatInfo(format);
            for (var ctInfo : info.mColorTypeInfos) {
                if (ctInfo.mColorType == colorType) {
                    mColorTypeToBackendFormat[colorType] = format;
                    return;
                }
            }
        }
    }

    public boolean hasUnifiedMemory() {
        return false;
    }

    @Override
    public boolean isFormatTexturable(BackendFormat format) {
        return false;
    }

    @Override
    public int getMaxRenderTargetSampleCount(BackendFormat format) {
        return 0;
    }

    @Override
    public boolean isFormatRenderable(int colorType, BackendFormat format, int sampleCount) {
        return false;
    }

    @Override
    public boolean isFormatRenderable(BackendFormat format, int sampleCount) {
        return false;
    }

    @Override
    public int getRenderTargetSampleCount(int sampleCount, BackendFormat format) {
        return 0;
    }

    @Override
    public long getSupportedWriteColorType(int dstColorType, ImageDesc dstDesc, int srcColorType) {
        return 0;
    }

    @Override
    protected long onSupportedReadColorType(int srcColorType, BackendFormat srcFormat, int dstColorType) {
        return 0;
    }

    @Override
    protected boolean onFormatCompatible(int colorType, BackendFormat format) {
        return false;
    }

    @Nullable
    @Override
    public ImageDesc getDefaultColorImageDesc(int imageType,
                                              int colorType,
                                              int width, int height, int depthOrArraySize,
                                              int mipLevelCount, int sampleCount, int imageFlags) {
        if (width < 1 || height < 1 || depthOrArraySize < 1 ||
                mipLevelCount < 0 || sampleCount < 0) {
            return null;
        }
        sampleCount = Math.max(1, sampleCount);
        int format = mColorTypeToBackendFormat[colorType];
        FormatInfo formatInfo = getFormatInfo(format);
        if (!formatInfo.isTexturable(VK_IMAGE_TILING_OPTIMAL) ||
                ((imageFlags & ISurface.FLAG_RENDERABLE) != 0 &&
                        !formatInfo.isRenderable(VK_IMAGE_TILING_OPTIMAL, sampleCount))) {
            return null;
        }

        //TODO

        final int depth;
        final int arraySize;
        switch (imageType) {
            case Engine.ImageType.k3D:
                depth = depthOrArraySize;
                arraySize = 1;
                break;
            case Engine.ImageType.k2DArray, Engine.ImageType.kCubeArray:
                depth = 1;
                arraySize = depthOrArraySize;
                break;
            default:
                depth = arraySize = 1;
                break;
        }

        if (width > mMaxTextureSize || height > mMaxTextureSize) {
            return null;
        }

        int maxMipLevels = DataUtils.computeMipLevelCount(width, height, depth);
        if (mipLevelCount == 0) {
            mipLevelCount = (imageFlags & ISurface.FLAG_MIPMAPPED) != 0
                    ? maxMipLevels
                    : 1; // only base level
        } else {
            mipLevelCount = Math.min(mipLevelCount, maxMipLevels);
        }

        if (sampleCount > 1 && mipLevelCount > 1) {
            return null;
        }

        int usage = VK_IMAGE_USAGE_SAMPLED_BIT |
                VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                VK_IMAGE_USAGE_TRANSFER_DST_BIT;
        if ((imageFlags & ISurface.FLAG_RENDERABLE) != 0) {
            usage |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
                    VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT;
        }

        return new VulkanImageDesc(
                0,
                VK_IMAGE_TYPE_2D,
                format,
                VK_IMAGE_TILING_OPTIMAL,
                usage,
                VK_SHARING_MODE_EXCLUSIVE,
                imageType,
                width, height, depth, arraySize,
                mipLevelCount, sampleCount, imageFlags
        );
    }

    @Nullable
    @Override
    protected BackendFormat onGetDefaultBackendFormat(int colorType) {
        return null;
    }

    @Nullable
    @Override
    public BackendFormat getCompressedBackendFormat(int compressionType) {
        return null;
    }

    @Nonnull
    @Override
    public PipelineKey_old makeDesc(PipelineKey_old desc, GpuRenderTarget renderTarget, GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        return null;
    }

    @Nonnull
    @Override
    public PipelineKey makeGraphicsPipelineKey(PipelineKey old, PipelineDesc pipelineDesc, RenderPassDesc renderPassDesc) {
        return null;
    }

    @Override
    protected short onGetReadSwizzle(ImageDesc desc, int colorType) {
        return 0;
    }

    @Override
    public short getWriteSwizzle(ImageDesc desc, int colorType) {
        return 0;
    }

    @Override
    public IResourceKey computeImageKey(ImageDesc desc, IResourceKey recycle) {
        if (desc instanceof VulkanImageDesc vulkanDesc) {
            return new VulkanImage.ResourceKey(vulkanDesc);
        }
        return null;
    }

    static int[] initSampleCounts(ContextOptions options,
                                  VkPhysicalDevice physDev,
                                  VkPhysicalDeviceProperties physProps,
                                  int format,
                                  int usage,
                                  MemoryStack stack) {
        stack.push();
        try {
            VkImageFormatProperties props = VkImageFormatProperties.malloc(stack);
            // when requesting MSAA support, we only consider 2D and Optimal
            int result = vkGetPhysicalDeviceImageFormatProperties(
                    physDev,
                    format,
                    VK_IMAGE_TYPE_2D,
                    VK_IMAGE_TILING_OPTIMAL,
                    usage,
                    0,
                    props
            );
            if (result != VK_SUCCESS) {
                options.mLogger.warn("Failed to vkGetPhysicalDeviceImageFormatProperties: {}",
                        VKUtil.getResultMessage(result));
                return IntArrays.EMPTY_ARRAY;
            }
            IntArrayList sampleCounts = new IntArrayList(5); // [1, 2, 4, 8, 16]
            int flags = props.sampleCounts();
            if ((flags & VK_SAMPLE_COUNT_1_BIT) != 0) {
                sampleCounts.add(1);
            }
            if ((flags & VK_SAMPLE_COUNT_2_BIT) != 0) {
                sampleCounts.add(2);
            }
            if ((flags & VK_SAMPLE_COUNT_4_BIT) != 0) {
                sampleCounts.add(4);
            }
            if ((flags & VK_SAMPLE_COUNT_8_BIT) != 0) {
                sampleCounts.add(8);
            }
            if ((flags & VK_SAMPLE_COUNT_16_BIT) != 0) {
                sampleCounts.add(16);
            }
            return sampleCounts.toIntArray();
        } finally {
            stack.pop();
        }
    }

    static class ColorTypeInfo {

        int mColorType = ColorInfo.CT_UNKNOWN;
        int mTransferColorType = ColorInfo.CT_UNKNOWN;

        static final int
                kUploadData_Flag = 0x1,
                kRenderable_Flag = 0x2; // renderable by engine
        int mFlags = 0;

        short mReadSwizzle = Swizzle.RGBA;
        short mWriteSwizzle = Swizzle.RGBA;

        @Override
        public String toString() {
            return "ColorTypeInfo{" +
                    "colorType=" + ColorInfo.colorTypeToString(mColorType) +
                    ", transferColorType=" + ColorInfo.colorTypeToString(mTransferColorType) +
                    ", flags=0x" + Integer.toHexString(mFlags) +
                    ", readSwizzle=" + Swizzle.toString(mReadSwizzle) +
                    ", writeSwizzle=" + Swizzle.toString(mWriteSwizzle) +
                    '}';
        }
    }

    static class FormatInfo {

        /*VkFormatFeatureFlags*/ int mOptimalTilingFeatures = 0;
        /*VkFormatFeatureFlags*/ int mLinearTilingFeatures = 0;

        int[] mColorSampleCounts = IntArrays.EMPTY_ARRAY;

        ColorTypeInfo[] mColorTypeInfos = {};

        void init(ContextOptions options,
                  VkPhysicalDevice physDev,
                  VkPhysicalDeviceProperties physProps,
                  int format,
                  MemoryStack stack) {
            stack.push();
            try {
                VkFormatProperties props = VkFormatProperties.calloc(stack);
                vkGetPhysicalDeviceFormatProperties(
                        physDev,
                        format,
                        props
                );
                mOptimalTilingFeatures = props.optimalTilingFeatures();
                mLinearTilingFeatures = props.linearTilingFeatures();

                if ((mOptimalTilingFeatures & VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT) != 0) {
                    // We make all renderable images support being used as input attachment
                    mColorSampleCounts = initSampleCounts(options,
                            physDev,
                            physProps,
                            format,
                            VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                                    VK_IMAGE_USAGE_TRANSFER_DST_BIT |
                                    VK_IMAGE_USAGE_SAMPLED_BIT |
                                    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
                                    VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT,
                            stack);
                }
            } finally {
                stack.pop();
            }
        }

        boolean isTexturable(int imageTiling) {
            return switch (imageTiling) {
                case VK_IMAGE_TILING_OPTIMAL -> (mOptimalTilingFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) != 0;
                case VK_IMAGE_TILING_LINEAR -> (mLinearTilingFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) != 0;
                default -> false;
            };
        }

        boolean isFilterable(int imageTiling) {
            return switch (imageTiling) {
                case VK_IMAGE_TILING_OPTIMAL ->
                        (mOptimalTilingFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) != 0;
                case VK_IMAGE_TILING_LINEAR ->
                        (mLinearTilingFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) != 0;
                default -> false;
            };
        }

        boolean isRenderable(int imageTiling, int sampleCount) {
            if (imageTiling == VK_IMAGE_TILING_OPTIMAL) {
                if (mColorSampleCounts.length == 0) {
                    return false;
                }
                return sampleCount <= mColorSampleCounts[mColorSampleCounts.length - 1];
            }
            return false;
        }

        boolean isStorage(int imageTiling) {
            return switch (imageTiling) {
                case VK_IMAGE_TILING_OPTIMAL -> (mOptimalTilingFeatures & VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT) != 0;
                case VK_IMAGE_TILING_LINEAR -> (mLinearTilingFeatures & VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT) != 0;
                default -> false;
            };
        }

        boolean isTransferSrc(int imageTiling) {
            return switch (imageTiling) {
                case VK_IMAGE_TILING_OPTIMAL -> (mOptimalTilingFeatures & VK_FORMAT_FEATURE_TRANSFER_SRC_BIT) != 0;
                case VK_IMAGE_TILING_LINEAR -> (mLinearTilingFeatures & VK_FORMAT_FEATURE_TRANSFER_SRC_BIT) != 0;
                default -> false;
            };
        }

        boolean isTransferDst(int imageTiling) {
            return switch (imageTiling) {
                case VK_IMAGE_TILING_OPTIMAL -> (mOptimalTilingFeatures & VK_FORMAT_FEATURE_TRANSFER_DST_BIT) != 0;
                case VK_IMAGE_TILING_LINEAR -> (mLinearTilingFeatures & VK_FORMAT_FEATURE_TRANSFER_DST_BIT) != 0;
                default -> false;
            };
        }

        @Override
        public String toString() {
            return "FormatInfo{" +
                    "optimalTilingFeatures=0x" + Integer.toHexString(mOptimalTilingFeatures) +
                    ", linearTilingFeatures=0x" + Integer.toHexString(mLinearTilingFeatures) +
                    ", colorSampleCounts=" + Arrays.toString(mColorSampleCounts) +
                    ", colorTypeInfos=" + Arrays.toString(mColorTypeInfos) +
                    '}';
        }
    }
}
