/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.awt;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.system.jawt.JAWTRectangle;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.LibFFI;
import org.lwjgl.system.macosx.MacOSXLibrary;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.vulkan.*;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.EXTMetalSurface.*;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author Fox
 * @author SWinxy
 */
class PlatformMacOSXVKCanvas {

    public static final String EXTENSION_NAME = VK_EXT_METAL_SURFACE_EXTENSION_NAME;

    /**
     * Creates the native Metal view.
     * <p>
     * Because {@link JNI} does not provide a method signature for {@code PPDDDDPP},
     * we have to construct a call interface ourselves via {@link LibFFI}.
     * <p>
     * {@code
     * id<MTLDevice> device = MTLCreateSystemDefaultDevice();
     * MTKView *view = [[MTKView alloc] initWithFrame:frame device:device]; // frame is from a GCRectMake();
     * surfaceLayers.layer = view.layer; // jawt platform object
     * return view.layer;
     * }
     *
     * @param platformInfo pointer to the jawt platform information struct
     * @param x            x position of the window
     * @param y            y position of the window
     * @param width        window width
     * @param height       window height
     * @return pointer to a native window handle
     */
    private static long createMTKView(long platformInfo, int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SharedLibrary metalKit = MacOSXLibrary.create("/System/Library/Frameworks/MetalKit.framework");
            SharedLibrary metal = MacOSXLibrary.create("/System/Library/Frameworks/Metal.framework");
            long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
            metalKit.getFunctionAddress("MTKView"); // loads the MTKView class or something (required, somehow)

            // id<MTLDevice> device = MTLCreateSystemDefaultDevice();
            long device = JNI.invokeP(metal.getFunctionAddress("MTLCreateSystemDefaultDevice"));


            PointerBuffer argumentTypes = BufferUtils.createPointerBuffer(7) // 4 arguments, one of them an array of
                    // 4 doubles
                    .put(0, LibFFI.ffi_type_pointer) // MTKView*
                    .put(1, LibFFI.ffi_type_pointer) // initWithFrame:
                    .put(2, LibFFI.ffi_type_double) // CGRect
                    .put(3, LibFFI.ffi_type_double) // CGRect
                    .put(4, LibFFI.ffi_type_double) // CGRect
                    .put(5, LibFFI.ffi_type_double) // CGRect
                    .put(6, LibFFI.ffi_type_pointer); // device*

            // Prepare the call interface
            FFICIF cif = FFICIF.malloc(stack);
            int status = LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_pointer, argumentTypes);
            if (status != LibFFI.FFI_OK) {
                throw new IllegalStateException("ffi_prep_cif failed: " + status);
            }

            // An array of pointers that point to the actual argument values.
            PointerBuffer arguments = stack.mallocPointer(7);

            // Storage for the actual argument values.
            ByteBuffer values = stack.malloc(
                    Pointer.POINTER_SIZE +     // MTKView*
                            Pointer.POINTER_SIZE +     // initWithFrame*
                            Double.BYTES * 4 +         // CGRect (4 doubles)
                            Pointer.POINTER_SIZE       // device*
            );

            // MTKView *view = [MTKView alloc];
            long mtkView = JNI.invokePPP(
                    ObjCRuntime.objc_getClass("MTKView"),
                    ObjCRuntime.sel_getUid("alloc"),
                    objc_msgSend);

            // Set up the argument buffers by inserting pointers

            // MTKView*
            arguments.put(MemoryUtil.memAddress(values));
            PointerBuffer.put(values, mtkView);

            // initWithFrame*
            arguments.put(MemoryUtil.memAddress(values));
            PointerBuffer.put(values, ObjCRuntime.sel_getUid("initWithFrame:"));

            // frame
            arguments.put(MemoryUtil.memAddress(values));
            values.putDouble(x);
            arguments.put(MemoryUtil.memAddress(values));
            values.putDouble(y);
            arguments.put(MemoryUtil.memAddress(values));
            values.putDouble(width);
            arguments.put(MemoryUtil.memAddress(values));
            values.putDouble(height);

            // device*
            arguments.put(MemoryUtil.memAddress(values));
            values.putLong(device);

            arguments.flip();
            values.flip();

            // [view initWithFrame:rect device:device];
            // Returns itself, we just need to know if it's NULL
            LongBuffer pMTKView = stack.mallocLong(1);
            LibFFI.ffi_call(cif, objc_msgSend, MemoryUtil.memByteBuffer(pMTKView), arguments);
            if (pMTKView.get(0) == MemoryUtil.NULL) {
                throw new IllegalStateException("[MTKView initWithFrame:device:] returned null.");
            }


            // layer = view.layer;
            long layer = JNI.invokePPP(mtkView,
                    ObjCRuntime.sel_getUid("layer"),
                    objc_msgSend);

            // set layer on JAWTSurfaceLayers object
            // surfaceLayers.layer = layer;
            JNI.invokePPPV(platformInfo,
                    ObjCRuntime.sel_getUid("setLayer:"),
                    layer,
                    objc_msgSend);

            // return layer;
            return layer;
        }
    }

    static long create(Canvas canvas, VkInstance instance) throws AWTException {
        try (AWT awt = new AWT(canvas)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {

                // if the canvas is inside e.g. a JSplitPane, the dsi coordinates are wrong and need to be corrected
                JAWTRectangle bounds = awt.getDrawingSurfaceInfo().bounds();
                int x = bounds.x();
                int y = bounds.y();

                JRootPane rootPane = SwingUtilities.getRootPane(canvas);
                if (rootPane != null) {
                    Point point = SwingUtilities.convertPoint(canvas, new Point(), rootPane);
                    x = point.x;
                    y = point.y;
                }

                // Get pointer to CAMetalLayer object representing the renderable surface
                long metalLayer = createMTKView(awt.getPlatformInfo(), x, y, bounds.width(), bounds.height());

                MacOSX.caFlush();

                VkMetalSurfaceCreateInfoEXT pCreateInfo = VkMetalSurfaceCreateInfoEXT
                        .calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_METAL_SURFACE_CREATE_INFO_EXT)
                        .pLayer(PointerBuffer.create(metalLayer, 1));

                LongBuffer pSurface = stack.mallocLong(1);
                int result = vkCreateMetalSurfaceEXT(instance, pCreateInfo, null, pSurface);

                switch (result) {
                    case VK_SUCCESS:
                        return pSurface.get(0);

                    // Possible VkResult codes returned
                    case VK_ERROR_OUT_OF_HOST_MEMORY:
                        throw new AWTException("Failed to create a Vulkan surface: a host memory allocation has " +
                                "failed.");
                    case VK_ERROR_OUT_OF_DEVICE_MEMORY:
                        throw new AWTException("Failed to create a Vulkan surface: a device memory allocation has " +
                                "failed.");

                        // vkCreateMetalSurfaceEXT return code
                    case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
                        throw new AWTException("Failed to create a Vulkan surface:" +
                                " the requested window is already in use by Vulkan or another API in a manner which " +
                                "prevents it from being used again.");

                        // Error unknown to the implementation
                    case VK_ERROR_UNKNOWN:
                        throw new AWTException("An unknown error has occurred;" +
                                " either the application has provided invalid input, or an implementation failure has" +
                                " occurred.");

                        // Unknown error not included in this list
                    default:
                        throw new AWTException("Calling vkCreateMetalSurfaceEXT failed with unknown Vulkan error: " + result);
                }
            }
        }
    }

    // On macOS, all physical devices and queue families must be capable of presentation with any layer.
    // As a result there is no macOS-specific query for these capabilities.
    static boolean checkSupport(VkPhysicalDevice physicalDevice, int queueFamilyIndex) {
        return true;
    }
}
