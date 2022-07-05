/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.graphics.vulkan.VkCore.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_EXT_BLEND_OPERATION_ADVANCED_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_VERSION_MAJOR;

/**
 * This class contains the shared global Vulkan objects, such as VkInstance, VkDevice and VkQueue,
 * which are re-used by CanvasContext. This class is created once and should be used by all vulkan
 * windowing contexts. The VulkanManager must be initialized before use.
 */
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

    private int mDriverVersion;

    private VulkanManager() {
    }

    @Nonnull
    public static VulkanManager getInstance() {
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
            TinyFileDialogs.tinyfd_messageBox("Failed to launch Modern UI",
                    "Vulkan is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);
            throw new RuntimeException("Vulkan is not supported");
        }

        final int version = VK.getInstanceVersionSupported();

        LOGGER.info(MARKER, "Vulkan version: {}.{}.{}",
                VK_VERSION_MAJOR(version), VK_VERSION_MINOR(version), VK_VERSION_PATCH(version));

        if (version < VK_API_VERSION_1_1) {
            TinyFileDialogs.tinyfd_messageBox("Failed to launch Modern UI",
                    "Vulkan 1.1 is not supported on your current platform. " +
                            "Make sure your operating system and graphics card drivers are up-to-date.",
                    "ok", "error", true);
            throw new RuntimeException("Vulkan 1.1 is not supported");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            CHECK(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pCount, null));
            final int count = pCount.get(0);
            final VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count, stack);
            CHECK(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pCount, properties));
            for (var prop : properties) {
                mInstanceExtensions.putIfAbsent(prop.extensionNameString(), prop.specVersion());
            }
        }

        LOGGER.info(MARKER, "Enumerated {} instance extensions", mInstanceExtensions.size());
        LOGGER.debug(MARKER, mInstanceExtensions);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer appName = stack.UTF8("Modern UI", true);
            final ByteBuffer engineName = stack.UTF8("Arc UI", true);
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
            CHECK_ERROR(vkCreateInstance(pCreateInfo, null, pInstance));
            mInstance = new VkInstance(pInstance.get(0), pCreateInfo);
        }

        LOGGER.info(MARKER, "Created Vulkan instance, Engine: {}", "Arc UI");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            CHECK(vkEnumeratePhysicalDevices(mInstance, pCount, null));
            final int deviceCount = pCount.get(0);
            if (deviceCount == 0) {
                throw new RuntimeException("No GPU device was found");
            }
            final PointerBuffer pPhysicalDevices = stack.mallocPointer(deviceCount);
            CHECK(vkEnumeratePhysicalDevices(mInstance, pCount, pPhysicalDevices));
            for (int i = 0; i < deviceCount; i++) {
                final var physicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), mInstance);
                if (choosePhysicalDeviceLocked(physicalDevice)) {
                    break;
                }
            }
        }

        if (mPhysicalDevice == null) {
            TinyFileDialogs.tinyfd_messageBox("Failed to launch Modern UI",
                    "You don't have a device with a Vulkan queue family that supports both graphics and compute.",
                    "ok", "error", true);
            throw new RuntimeException("No suitable physical device was found");
        }

        final PointerBuffer extensionNames;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pCount = stack.mallocInt(1);
            CHECK(vkEnumerateDeviceExtensionProperties(mPhysicalDevice, (ByteBuffer) null, pCount, null));
            final int count = pCount.get(0);
            final VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count, stack);
            CHECK(vkEnumerateDeviceExtensionProperties(mPhysicalDevice, (ByteBuffer) null, pCount, properties));
            extensionNames = memAllocPointer(count);
            for (var prop : properties) {
                extensionNames.put(prop.extensionName());
                mDeviceExtensions.putIfAbsent(prop.extensionNameString(), prop.specVersion());
            }
            extensionNames.flip();
        }

        LOGGER.info(MARKER, "Enumerated {} device extensions", mDeviceExtensions.size());
        LOGGER.debug(MARKER, mDeviceExtensions);

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
                    .pNext(mPhysicalDeviceFeatures2)
                    .pQueueCreateInfos(queueInfos)
                    .ppEnabledExtensionNames(extensionNames);

            final PointerBuffer pDevice = stack.mallocPointer(1);
            CHECK(vkCreateDevice(mPhysicalDevice, pCreateInfo, null, pDevice));
            mDevice = new VkDevice(pDevice.get(0), mPhysicalDevice, pCreateInfo, VK_API_VERSION_1_1);
        } finally {
            memFree(extensionNames);
        }

        LOGGER.info(MARKER, "Created Vulkan device, Queue index: {}", mGraphicsQueueIndex);
    }

    private boolean choosePhysicalDeviceLocked(@Nonnull VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2
                    .calloc(stack)
                    .sType$Default();
            vkGetPhysicalDeviceProperties2(physicalDevice, properties2);
            final VkPhysicalDeviceProperties properties = properties2.properties();

            LOGGER.info(MARKER, "List device ID {}, Name: {}, Type: {}", properties.deviceID(),
                    properties.deviceNameString(), getPhysicalDeviceTypeName(properties.deviceType()));

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
            mDriverVersion = driverVersion;
            LOGGER.info(MARKER, "Choose device ID {}, vendor ID: {}, driver version: {}",
                    properties.deviceID(), switch (vendorID) {
                        case 0x1002 -> "AMD";
                        case 0x1010 -> "ImgTec";
                        case 0x10DE -> "NVIDIA";
                        case 0x13B5 -> "ARM";
                        case 0x5143 -> "Qualcomm";
                        case 0x8086 -> "INTEL";
                        default -> "0x" + Integer.toHexString(vendorID);
                    }, switch (vendorID) {
                        case 0x10DE -> String.format("%d.%d.%d.%d", // NVIDIA
                                driverVersion >>> 22,
                                (driverVersion >>> 14) & 0xFF,
                                (driverVersion >> 6) & 0xFF,
                                driverVersion & 0x3F);
                        default -> "0x" + Integer.toHexString(vendorID);
                    });
            return true;
        }
    }

    @Override
    public synchronized void close() {
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
    public static void freeFeaturesExtensionsStructs(@Nonnull VkPhysicalDeviceFeatures2 features) {
        long pNext = features.pNext();
        while (pNext != NULL) {
            long current = pNext;
            pNext = VkPhysicalDeviceFeatures2.npNext(current);
            nmemFree(current);
        }
    }
}
