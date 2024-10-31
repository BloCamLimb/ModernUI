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

package icyllis.arc3d.test.vulkan;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ContextOptions;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.vulkan.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

public class TestVulkanInit implements AutoCloseable {

    private final Logger mLogger;

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

    public TestVulkanInit(Logger logger) {
        mLogger = logger;
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
            TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "Vulkan is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);
            throw new RuntimeException("Vulkan is not supported");
        }

        final int version = VK.getInstanceVersionSupported();

        mLogger.info("Vulkan version: {}.{}.{}",
                VK_VERSION_MAJOR(version), VK_VERSION_MINOR(version), VK_VERSION_PATCH(version));

        if (version < VK_API_VERSION_1_1) {
            TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "Vulkan 1.1 is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);
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

        mLogger.info("Enumerated {} instance extensions", mInstanceExtensions.size());
        mLogger.debug(String.valueOf(mInstanceExtensions));

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

        mLogger.info("Created Vulkan instance, Engine: {}", "Arc3D");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            VKUtil._CHECK_(vkEnumeratePhysicalDevices(mInstance, pCount, null));
            final int deviceCount = pCount.get(0);
            if (deviceCount == 0) {
                throw new RuntimeException("No GPU device was found");
            }
            final PointerBuffer pPhysicalDevices = stack.mallocPointer(deviceCount);
            VKUtil._CHECK_(vkEnumeratePhysicalDevices(mInstance, pCount, pPhysicalDevices));
            for (int i = 0; i < deviceCount; i++) {
                final var physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), mInstance);
                if (choosePhysicalDeviceLocked(physicalDevice)) {
                    break;
                }
            }
        }

        if (mPhysicalDevice == null) {
            TinyFileDialogs.tinyfd_messageBox("Failed to launch ModernUI",
                    "You don't have a device with a Vulkan queue family that supports both graphics and compute.",
                    "ok", "error", true);
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
                extensionNames.put(prop.extensionName());
                mDeviceExtensions.putIfAbsent(prop.extensionNameString(), prop.specVersion());
            }
            extensionNames.flip();
        }

        mLogger.info("Enumerated {} device extensions", mDeviceExtensions.size());
        mLogger.debug(String.valueOf(mDeviceExtensions));

        mPhysicalDeviceFeatures2 = VkPhysicalDeviceFeatures2
                .calloc()
                .sType$Default();

        if (mDeviceExtensions.getInt(VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME) >= 2) {
            mLogger.info("Enabled {}", VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME);
            mPhysicalDeviceFeatures2.pNext(VkPhysicalDeviceBlendOperationAdvancedFeaturesEXT
                    .calloc()
                    .sType$Default());
        } else {
            mLogger.info("Disabled {}", VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME);
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

        mLogger.info("Created Vulkan device, Queue index: {}", mGraphicsQueueIndex);
    }

    private boolean choosePhysicalDeviceLocked(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2
                    .calloc(stack)
                    .sType$Default();
            vkGetPhysicalDeviceProperties2(physicalDevice, properties2);
            final VkPhysicalDeviceProperties properties = properties2.properties();

            mLogger.info("List device ID {}, Name: {}, Type: {}", properties.deviceID(),
                    properties.deviceNameString(), VKUtil.getPhysicalDeviceTypeName(properties.deviceType()));

            if (properties.apiVersion() < VK_API_VERSION_1_1) {
                mLogger.info("Skip device ID {} because it does not support Vulkan 1.1",
                        properties.deviceID());
                return false;
            }

            final IntBuffer pCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pCount, null);
            final int count = pCount.get(0);
            if (count == 0) {
                mLogger.info("Skip device ID {} because no queue family was found",
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
                mLogger.info("Skip device ID {} because no suitable queue family was found",
                        properties.deviceID());
                return false;
            }
            // XXX: we assume the graphics queue can present things

            mPhysicalDevice = physicalDevice;
            int vendorID = properties.vendorID();
            int driverVersion = properties.driverVersion();
            mPhysicalDeviceVersion = properties.apiVersion();
            mDriverVersion = driverVersion;
            mLogger.info("Choose device ID {}, vendor ID: {}, driver version: {}",
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

    @SharedPtr
    public ImmediateContext createContext(ContextOptions options) {
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

    @Override
    public void close() {
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

        mLogger.info("Terminated VulkanManager");
    }

    /**
     * @return the vendor-specified version of the driver
     */
    public int getDriverVersion() {
        return mDriverVersion;
    }

    /**
     * All Vulkan structs that could be part of the features chain will start with the
     * structure type followed by the pNext pointer. We cast to the CommonVulkanHeader,
     * so we can get access to the pNext for the next struct.
     *
     * <pre>{@code
     * struct CommonVulkanHeader {
     *         VkStructureType sType;
     *         void* pNext;
     *     };
     * }</pre>
     *
     * @param features the features whose chain structs to be freed
     */
    public static void freeFeaturesExtensionsStructs(VkPhysicalDeviceFeatures2 features) {
        long pNext = features.pNext();
        while (pNext != NULL) {
            long current = pNext;
            pNext = VkPhysicalDeviceFeatures2.npNext(current);
            nmemFree(current);
        }
    }
}
