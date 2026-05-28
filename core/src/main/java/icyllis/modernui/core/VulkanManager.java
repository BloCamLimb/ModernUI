/*
 * Modern UI.
 * Copyright (C) 2022-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ContextOptions;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.vulkan.VKUtil;
import icyllis.arc3d.vulkan.VulkanBackendContext;
import icyllis.arc3d.vulkan.VulkanMemoryAllocator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static icyllis.arc3d.vulkan.VKUtil.*;
import static icyllis.modernui.util.Log.LOGGER;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK11.*;

/**
 * This class contains the shared global Vulkan objects, such as VkInstance, VkDevice and VkQueue,
 * which are re-used by CanvasContext. This class is created once and should be used by all vulkan
 * windowing contexts. The VulkanManager must be initialized before use.
 */
@ApiStatus.Internal
public final class VulkanManager implements AutoCloseable {

    private static volatile VulkanManager sInstance;

    private VkInstance mInstance;
    private VkPhysicalDevice mPhysicalDevice;
    private VkDevice mDevice;

    private int mGraphicsQueueIndex = -1;
    private int mComputeQueueIndex = -1;

    private final Object2IntOpenHashMap<String> mInstanceExtensions = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<String> mDeviceExtensions = new Object2IntOpenHashMap<>();
    private VkPhysicalDeviceFeatures2 mPhysicalDeviceFeatures2;

    private int mPhysicalDeviceVersion;
    private int mDriverVersion;

    private VulkanMemoryAllocator mMemoryAllocator;

    private VulkanManager() {
    }

    @NonNull
    public static VulkanManager get() {
        if (sInstance == null) {
            synchronized (VulkanManager.class) {
                if (sInstance == null) {
                    sInstance = new VulkanManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Sets up the vulkan context that is shared among all clients of the VulkanManager. This must
     * be call once before use of the VulkanManager. Multiple calls after the first will simply
     * return.
     */
    public synchronized void initialize() {
        if (mDevice != null) {
            return;
        }

        if (!GLFWVulkan.glfwVulkanSupported()) {
            /*TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "Vulkan is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);*/
            throw new RuntimeException("Vulkan is not supported");
        }

        final int version = VK.getInstanceVersionSupported();

        LOGGER.info(MARKER, "Vulkan version: {}.{}.{}",
                VK_VERSION_MAJOR(version), VK_VERSION_MINOR(version), VK_VERSION_PATCH(version));

        if (version < VK_API_VERSION_1_1) {
            /*TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "Vulkan 1.1 is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);*/
            throw new RuntimeException("Vulkan 1.1 is not supported");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            VKUtil._CHECK_(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pCount, null));
            final int count = pCount.get(0);
            final VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count, stack);
            VKUtil._CHECK_(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pCount, properties));
            for (var prop : properties) {
                mInstanceExtensions.putIfAbsent(prop.extensionNameString(), prop.specVersion());
            }
        }

        LOGGER.info(MARKER, "Enumerated {} instance extensions", mInstanceExtensions.size());
        if (LOGGER.isDebugEnabled(MARKER)) {
            LOGGER.debug(MARKER, String.valueOf(mInstanceExtensions));
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer appName = stack.UTF8("ModernUI", true);
            final ByteBuffer engineName = stack.UTF8("Arc3D", true);
            final VkApplicationInfo appInfo = VkApplicationInfo
                    .calloc(stack)
                    .sType$Default()
                    .pApplicationName(appName)
                    .pEngineName(engineName)
                    .apiVersion(version);

            final VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo
                    .calloc(stack)
                    .sType$Default()
                    .pApplicationInfo(appInfo)
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(GLFWVulkan.glfwGetRequiredInstanceExtensions());

            final PointerBuffer pInstance = stack.mallocPointer(1);
            VKUtil._CHECK_ERROR_(vkCreateInstance(pCreateInfo, null, pInstance));
            mInstance = new VkInstance(pInstance.get(0), pCreateInfo);
        }

        LOGGER.info(MARKER, "Created Vulkan instance, Engine: {}", "Arc3D");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            VKUtil._CHECK_(vkEnumeratePhysicalDevices(mInstance, pCount, null));
            final int deviceCount = pCount.get(0);
            if (deviceCount == 0) {
                throw new RuntimeException("No GPU device was found");
            }
            final PointerBuffer pPhysicalDevices = stack.mallocPointer(deviceCount);
            VKUtil._CHECK_(vkEnumeratePhysicalDevices(mInstance, pCount, pPhysicalDevices));
            boolean found = false;
            for (int i = 0; i < deviceCount; i++) {
                final var physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), mInstance);
                found = choosePhysicalDeviceLocked(physicalDevice, found);
            }
        }

        if (mPhysicalDevice == null) {
            /*TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "You don't have a device with a Vulkan queue family that supports both graphics and compute.",
                    "ok", "error", true);*/
            throw new RuntimeException("No suitable physical device was found");
        }

        final PointerBuffer extensionNames;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            VKUtil._CHECK_(vkEnumerateDeviceExtensionProperties(mPhysicalDevice, (ByteBuffer) null, pCount, null));
            final int count = pCount.get(0);
            final VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count, stack);
            VKUtil._CHECK_(vkEnumerateDeviceExtensionProperties(mPhysicalDevice, (ByteBuffer) null, pCount,
                    properties));
            extensionNames = memAllocPointer(count);
            for (var prop : properties) {
                String name = prop.extensionNameString();
                extensionNames.put(prop.extensionName());
                mDeviceExtensions.putIfAbsent(name, prop.specVersion());
            }
            extensionNames.flip();
        }

        LOGGER.info(MARKER, "Enumerated {} device extensions", mDeviceExtensions.size());
        if (LOGGER.isDebugEnabled(MARKER)) {
            LOGGER.debug(MARKER, String.valueOf(mDeviceExtensions));
        }

        if (mPhysicalDeviceFeatures2 != null) {
            throw new IllegalStateException();
        }
        mPhysicalDeviceFeatures2 = VkPhysicalDeviceFeatures2
                .calloc()
                .sType$Default();

        if (mDeviceExtensions.getInt(VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME) >= 2) {
            LOGGER.info(MARKER, "Enabled {}", VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME);
            mPhysicalDeviceFeatures2.pNext(VkPhysicalDeviceBlendOperationAdvancedFeaturesEXT
                    .calloc()
                    .sType$Default());
        } else {
            LOGGER.info(MARKER, "Disabled {}", VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME);
        }

        vkGetPhysicalDeviceFeatures2(mPhysicalDevice, mPhysicalDeviceFeatures2);
        // this is slow, just disable it
        mPhysicalDeviceFeatures2.features().robustBufferAccess(false);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkDeviceQueueCreateInfo.Buffer queueInfos = VkDeviceQueueCreateInfo
                    .calloc(1, stack)
                    .sType$Default()
                    .queueFamilyIndex(mGraphicsQueueIndex)
                    .pQueuePriorities(stack.floats(0.0f));

            final VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo
                    .calloc(stack)
                    .sType$Default()
                    .pNext(mPhysicalDeviceFeatures2.address())
                    .pQueueCreateInfos(queueInfos)
                    .ppEnabledExtensionNames(extensionNames);

            final PointerBuffer pDevice = stack.mallocPointer(1);
            VKUtil._CHECK_(vkCreateDevice(mPhysicalDevice, pCreateInfo, null, pDevice));
            mDevice = new VkDevice(pDevice.get(0), mPhysicalDevice, pCreateInfo, VK_API_VERSION_1_1);
        } finally {
            memFree(extensionNames);
        }

        LOGGER.info(MARKER, "Created Vulkan device, Queue index: {}", mGraphicsQueueIndex);
    }

    private boolean choosePhysicalDeviceLocked(VkPhysicalDevice physicalDevice, boolean printOnly) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2
                    .calloc(stack)
                    .sType$Default();
            vkGetPhysicalDeviceProperties2(physicalDevice, properties2);
            final VkPhysicalDeviceProperties properties = properties2.properties();
            int apiVersion = properties.apiVersion();

            LOGGER.info(MARKER, "List device ID {}, Name: {}, API Version: {}.{}.{}, Type: {}", properties.deviceID(),
                    properties.deviceNameString(),
                    VK_VERSION_MAJOR(apiVersion), VK_VERSION_MINOR(apiVersion), VK_VERSION_PATCH(apiVersion),
                    VKUtil.getPhysicalDeviceTypeName(properties.deviceType()));

            if (printOnly) {
                return true;
            }

            if (properties.apiVersion() < VK_API_VERSION_1_1) {
                LOGGER.info(MARKER, "Skip device ID {} because it does not support Vulkan 1.1",
                        properties.deviceID());
                return false;
            }

            final IntBuffer pCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pCount, null);
            final int count = pCount.get(0);
            if (count == 0) {
                LOGGER.info(MARKER, "Skip device ID {} because no queue family was found",
                        properties.deviceID());
                return false;
            }
            final VkQueueFamilyProperties.Buffer queues = VkQueueFamilyProperties.malloc(count, stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pCount, queues);

            mGraphicsQueueIndex = -1;
            mComputeQueueIndex = -1;
            for (int j = 0; j < count; j++) {
                final VkQueueFamilyProperties queue = queues.get(j);
                if (queue.queueCount() == 0) {
                    continue;
                }
                int flags = queue.queueFlags();
                if (mGraphicsQueueIndex == -1 && (flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    mGraphicsQueueIndex = j;
                }
                if (mComputeQueueIndex == -1 && (flags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    mComputeQueueIndex = j;
                }
                if (mGraphicsQueueIndex > 0 && mComputeQueueIndex > 0) {
                    break;
                }
            }
            if (mGraphicsQueueIndex == -1 || mComputeQueueIndex == -1) {
                LOGGER.info(MARKER, "Skip device ID {} because no suitable queue family was found",
                        properties.deviceID());
                return false;
            }
            // XXX: we assume the graphics queue can present things

            mPhysicalDevice = physicalDevice;
            int vendorID = properties.vendorID();
            int driverVersion = properties.driverVersion();
            mPhysicalDeviceVersion = properties.apiVersion();
            mDriverVersion = driverVersion;
            LOGGER.info(MARKER, "Choose device ID {}, vendor ID: {}, driver version: {}",
                    properties.deviceID(), VKUtil.getVendorIDName(vendorID),
                    switch (vendorID) {
                        case VKUtil.kNVIDIA_VendorID -> String.format("%d.%d.%d.%d",
                                driverVersion >>> 22,
                                (driverVersion >>> 14) & 0xFF,
                                (driverVersion >> 6) & 0xFF,
                                driverVersion & 0x3F);
                        default -> "0x" + Integer.toHexString(driverVersion);
                    });
            return true;
        }
    }

    @Nullable
    @SharedPtr
    public ImmediateContext createContext(@NonNull ContextOptions options) {
        VulkanBackendContext backendContext = new VulkanBackendContext();
        backendContext.mInstance = mInstance;
        backendContext.mPhysicalDevice = mPhysicalDevice;
        backendContext.mDevice = mDevice;
        backendContext.mGraphicsQueueIndex = mGraphicsQueueIndex;
        backendContext.mDeviceFeatures2 = mPhysicalDeviceFeatures2;
        if (mMemoryAllocator == null) {
            mMemoryAllocator = VulkanMemoryAllocator.make(
                    mInstance, mPhysicalDevice, mDevice, mPhysicalDeviceVersion,
                    0
            );
        }
        backendContext.mMemoryAllocator = mMemoryAllocator;
        try (var stack = MemoryStack.stackPush()) {
            var pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(mDevice, mGraphicsQueueIndex, 0, pQueue);
            backendContext.mQueue = new VkQueue(pQueue.get(0), mDevice);
        }
        return VKUtil.makeVulkan(backendContext, options);
    }

    // transfer ownership
    public void setMemoryAllocator(VulkanMemoryAllocator memoryAllocator) {
        if (mMemoryAllocator == null) {
            mMemoryAllocator = memoryAllocator;
        } else {
            throw new IllegalStateException();
        }
    }

    // memory is managed by this
    public VulkanMemoryAllocator getMemoryAllocator() {
        return mMemoryAllocator;
    }

    /**
     * @return the vendor-specified version of the driver
     */
    public int getDriverVersion() {
        return mDriverVersion;
    }

    public VkInstance getInstance() {
        return mInstance;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return mPhysicalDevice;
    }

    public VkDevice getDevice() {
        return mDevice;
    }

    public int getGraphicsQueueIndex() {
        return mGraphicsQueueIndex;
    }

    public int getPhysicalDeviceVersion() {
        return mPhysicalDeviceVersion;
    }

    // transfer ownership
    public void setPhysicalDeviceFeatures2(VkPhysicalDeviceFeatures2 physicalDeviceFeatures2) {
        if (mPhysicalDeviceFeatures2 == null) {
            mPhysicalDeviceFeatures2 = physicalDeviceFeatures2;
        } else {
            throw new IllegalStateException();
        }
    }

    // memory is managed by this
    public VkPhysicalDeviceFeatures2 getPhysicalDeviceFeatures2() {
        return mPhysicalDeviceFeatures2;
    }

    @Override
    public synchronized void close() {
        if (mMemoryAllocator != null) {
            mMemoryAllocator.close();
        }

        if (mDevice != null) {
            vkDeviceWaitIdle(mDevice);
            vkDestroyDevice(mDevice, null);
        }

        if (mInstance != null) {
            vkDestroyInstance(mInstance, null);
        }

        mGraphicsQueueIndex = -1;
        mComputeQueueIndex = -1;
        mDevice = null;
        mPhysicalDevice = null;
        mInstance = null;
        mMemoryAllocator = null;
        mInstanceExtensions.clear();
        mInstanceExtensions.trim();
        mDeviceExtensions.clear();
        mDeviceExtensions.trim();
        if (mPhysicalDeviceFeatures2 != null) {
            freeFeaturesExtensionsStructs(mPhysicalDeviceFeatures2);
            mPhysicalDeviceFeatures2.free();
        }
        mPhysicalDeviceFeatures2 = null;

        LOGGER.info(MARKER, "Terminated VulkanManager");
    }

    /**
     * All Vulkan structs that could be part of the features chain will start with the
     * structure type followed by the pNext pointer. We cast to the VkBaseInStructure,
     * so we can get access to the pNext for the next struct.
     *
     * <pre>{@code
     * struct VkBaseInStructure {
     *         VkStructureType sType;
     *         void* pNext;
     *     };
     * }</pre>
     *
     * @param features the features whose chain structs to be freed
     */
    public static void freeFeaturesExtensionsStructs(@NonNull VkPhysicalDeviceFeatures2 features) {
        long pNext = features.pNext();
        while (pNext != NULL) {
            long current = pNext;
            pNext = VkPhysicalDeviceFeatures2.npNext(current);
            nmemFree(current);
        }
    }
}
