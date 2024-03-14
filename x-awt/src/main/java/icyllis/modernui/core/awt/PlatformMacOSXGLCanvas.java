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
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jawt.*;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.macosx.ObjCRuntime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.nio.*;

import static org.lwjgl.opengl.CGL.*;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.Pointer.POINTER_SIZE;
import static org.lwjgl.system.jawt.JAWTFunctions.*;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.lwjgl.system.macosx.ObjCRuntime.*;

public class PlatformMacOSXGLCanvas implements PlatformGLCanvas {
    private static final int NSOpenGLPFAAllRenderers = 1;    /* choose from all available renderers          */
    private static final int NSOpenGLPFATripleBuffer = 3;    /* choose a triple buffered pixel format        */
    private static final int NSOpenGLPFADoubleBuffer = 5;    /* choose a double buffered pixel format        */
    private static final int NSOpenGLPFAAuxBuffers = 7;    /* number of aux buffers                        */
    private static final int NSOpenGLPFAColorSize = 8;    /* number of color buffer bits                  */
    private static final int NSOpenGLPFAAlphaSize = 11;    /* number of alpha component bits               */
    private static final int NSOpenGLPFADepthSize = 12;    /* number of depth buffer bits                  */
    private static final int NSOpenGLPFAStencilSize = 13;    /* number of stencil buffer bits                */
    private static final int NSOpenGLPFAAccumSize = 14;    /* number of accum buffer bits                  */
    private static final int NSOpenGLPFAMinimumPolicy = 51;    /* never choose smaller buffers than requested  */
    private static final int NSOpenGLPFAMaximumPolicy = 52;    /* choose largest buffers of type requested     */
    private static final int NSOpenGLPFASampleBuffers = 55;    /* number of multi sample buffers               */
    private static final int NSOpenGLPFASamples = 56;    /* number of samples per multi sample buffer    */
    private static final int NSOpenGLPFAAuxDepthStencil = 57;    /* each aux buffer has its own depth stencil    */
    private static final int NSOpenGLPFAColorFloat = 58;    /* color buffers store floating point pixels    */
    private static final int NSOpenGLPFAMultisample = 59;    /* choose multisampling                         */
    private static final int NSOpenGLPFASupersample = 60;    /* choose supersampling                         */
    private static final int NSOpenGLPFASampleAlpha = 61;    /* request alpha filtering                      */
    private static final int NSOpenGLPFARendererID = 70;    /* request renderer by ID                       */
    private static final int NSOpenGLPFANoRecovery = 72;    /* disable all failure recovery systems         */
    private static final int NSOpenGLPFAAccelerated = 73;    /* choose a hardware accelerated renderer       */
    private static final int NSOpenGLPFAClosestPolicy = 74;    /* choose the closest color buffer to request   */
    private static final int NSOpenGLPFABackingStore = 76;    /* back buffer contents are valid after swap    */
    private static final int NSOpenGLPFAScreenMask = 84;    /* bit mask of supported physical screens       */
    private static final int NSOpenGLPFAAllowOfflineRenderers = 96;  /* allow use of offline renderers               */
    private static final int NSOpenGLPFAAcceleratedCompute = 97;    /* choose a hardware accelerated compute device */
    private static final int NSOpenGLPFAOpenGLProfile = 99;    /* specify an OpenGL Profile to use             */
    private static final int NSOpenGLPFAVirtualScreenCount = 128;    /* number of virtual screens in this format     */

    private static final int NSOpenGLPFAStereo = 6;
    private static final int NSOpenGLPFAOffScreen = 53;
    private static final int NSOpenGLPFAFullScreen = 54;
    private static final int NSOpenGLPFASingleRenderer = 71;
    private static final int NSOpenGLPFARobust = 75;
    private static final int NSOpenGLPFAMPSafe = 78;
    private static final int NSOpenGLPFAWindow = 80;
    private static final int NSOpenGLPFAMultiScreen = 81;
    private static final int NSOpenGLPFACompliant = 83;
    private static final int NSOpenGLPFAPixelBuffer = 90;
    private static final int NSOpenGLPFARemotePixelBuffer = 91;

    private static final int NSOpenGLProfileVersion3_2Core = 0x3200;
    private static final int NSOpenGLProfileVersionLegacy = 0x1000;
    private static final int NSOpenGLProfileVersion4_1Core = 0x4100;

    public static final JAWT awt;
    private static final long objc_msgSend;
    private static final long NSOpenGLPixelFormat;

    static {
        awt = JAWT.calloc();
        awt.version(JAWT_VERSION_1_7);
        if (!JAWT_GetAWT(awt))
            throw new AssertionError("GetAWT failed");
        objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        NSOpenGLPixelFormat = objc_getClass("NSOpenGLPixelFormat");
    }

    public JAWTDrawingSurface ds;
    private long view;
    private int width;
    private int height;

    @Override
    public long create(Canvas canvas, GLData attribs, GLData effective) throws AWTException {
        this.ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        canvas.addHierarchyListener(e -> {
            // if the canvas, or a parent component is hidden/shown, we must update the hidden state of the layer
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0) {
                long layer = invokePPP(view, sel_getUid("layer"), objc_msgSend);
                invokePPPV(layer, sel_getUid("setHidden:"), e.getChanged().isShowing() ? 0 : 1, objc_msgSend);
                // flush the new state to the CoreAnimation pipeline, to actually get the new state displayed
                MacOSX.caFlush();
            }
        });
        JAWTDrawingSurface ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        try {
            int lock = JAWT_DrawingSurface_Lock(ds, ds.Lock());
            if ((lock & JAWT_LOCK_ERROR) != 0)
                throw new AWTException("JAWT_DrawingSurface_Lock() failed");
            try {
                JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
                try {
                    // if the canvas is inside e.g. a JSplitPane, the dsi coordinates are wrong and need to be corrected
                    JRootPane rootPane = SwingUtilities.getRootPane(canvas);
                    if (rootPane != null) {
                        Point point = SwingUtilities.convertPoint(canvas, new Point(), rootPane);
                        dsi.bounds().x(point.x);
                        dsi.bounds().y(point.y);
                    }
                    width = dsi.bounds().width();
                    height = dsi.bounds().height();

                    //TODO: we don't really need 100
                    ByteBuffer attribsArray = ByteBuffer.allocateDirect(4 * 100).order(ByteOrder.nativeOrder());
                    attribsArray.putInt(NSOpenGLPFAAccelerated);
                    attribsArray.putInt(NSOpenGLPFAClosestPolicy);
                    if (attribs.stereo) {
                        attribsArray.putInt(NSOpenGLPFAStereo);
                    }
                    if (attribs.doubleBuffer) {
                        //doesn't work currently
                        //attribsArray.putInt(NSOpenGLPFADoubleBuffer);
                    }

                    if (attribs.pixelFormatFloat) {
                        attribsArray.putInt(NSOpenGLPFAColorFloat);
                    }

                    attribsArray.putInt(NSOpenGLPFAAccumSize);
                    attribsArray.putInt(attribs.accumRedSize + attribs.accumGreenSize + attribs.accumBlueSize + attribs.accumAlphaSize);

                    int colorBits = attribs.redSize +
                            attribs.greenSize +
                            attribs.blueSize;

                    // macOS needs non-zero color size, so set reasonable values
                    if (colorBits == 0)
                        colorBits = 24;
                    else if (colorBits < 15)
                        colorBits = 15;

                    attribsArray.putInt(NSOpenGLPFAColorSize);
                    attribsArray.putInt(colorBits);

                    attribsArray.putInt(NSOpenGLPFAAlphaSize);
                    attribsArray.putInt(attribs.alphaSize);

                    attribsArray.putInt(NSOpenGLPFADepthSize);
                    attribsArray.putInt(attribs.depthSize);

                    attribsArray.putInt(NSOpenGLPFAStencilSize);
                    attribsArray.putInt(attribs.stencilSize);

                    if (attribs.samples == 0) {
                        attribsArray.putInt(NSOpenGLPFASampleBuffers);
                        attribsArray.putInt(0);
                    } else {
                        attribsArray.putInt(NSOpenGLPFASampleBuffers);
                        attribsArray.putInt(1);
                        attribsArray.putInt(NSOpenGLPFASamples);
                        attribsArray.putInt(attribs.samples);
                    }

                    if (attribs.profile == GLData.Profile.CORE) {
                        attribsArray.putInt(NSOpenGLPFAOpenGLProfile);
                        attribsArray.putInt(NSOpenGLProfileVersion3_2Core);
                    }
                    if (attribs.profile == GLData.Profile.COMPATIBILITY) {
                        attribsArray.putInt(NSOpenGLPFAOpenGLProfile);
                        attribsArray.putInt(NSOpenGLProfileVersionLegacy);
                    } else {
                        if (attribs.majorVersion >= 4) {
                            attribsArray.putInt(NSOpenGLPFAOpenGLProfile);
                            attribsArray.putInt(NSOpenGLProfileVersion4_1Core);
                        } else if (attribs.majorVersion >= 3) {
                            attribsArray.putInt(NSOpenGLPFAOpenGLProfile);
                            attribsArray.putInt(NSOpenGLProfileVersion3_2Core);
                        } else {
                            attribsArray.putInt(NSOpenGLPFAOpenGLProfile);
                            attribsArray.putInt(NSOpenGLProfileVersionLegacy);
                        }
                    }

                    // 0 Terminated
                    attribsArray.putInt(0).rewind();

                    long pixelFormat = invokePPP(NSOpenGLPixelFormat, sel_getUid("alloc"), objc_msgSend);
                    pixelFormat = invokePPPP(pixelFormat, sel_getUid("initWithAttributes:"),
                            MemoryUtil.memAddress(attribsArray), objc_msgSend);

                    view = createNSOpenGLView(dsi.platformInfo(), pixelFormat, dsi.bounds().x(), dsi.bounds().y(),
                            width, height);
                    MacOSX.caFlush();
                    long openGLContext = invokePPP(view, sel_getUid("openGLContext"), objc_msgSend);
                    return invokePPP(openGLContext, sel_getUid("CGLContextObj"), objc_msgSend);
                } finally {
                    JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                }
            } finally {
                JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
            }
        } finally {
            JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
        }
    }

    private long createNSOpenGLView(long platformInfo, long pixelFormat, int x, int y, int width, int height) {
        long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");

        // get offset in window from JAWTSurfaceLayers
        long H = JNI.invokePPPPP(platformInfo,
                ObjCRuntime.sel_getUid("windowLayer"),
                ObjCRuntime.sel_getUid("frame"),
                ObjCRuntime.sel_getUid("size"),
                objc_msgSend);
        // height is the 4th member of the 4*64bit struct
        double h = MemoryUtil.memGetDouble(H + 3 * 8);

        // MTKView *view = [[MTKView alloc] initWithFrame:frame device:device];
        // get MTKView class and allocate instance
        long NSOpenGLView = ObjCRuntime.objc_getClass("NSOpenGLView");
        long nsOpenGLView = JNI.invokePPP(NSOpenGLView,
                ObjCRuntime.sel_getUid("alloc"),
                objc_msgSend);

        final double[] frame = new double[]{x, y, width, height};
        // init MTKView with frame and device
        long view = NSOpenGLView_initWithFrame(nsOpenGLView, frame, pixelFormat);

        JNI.invokePPV(nsOpenGLView,
                ObjCRuntime.sel_getUid("setWantsLayer:"),
                true,
                objc_msgSend);

        // get layer from NSOpenGLView instance
        long openglViewLayer = JNI.invokePPJ(nsOpenGLView,
                ObjCRuntime.sel_getUid("layer"),
                objc_msgSend);

        // set layer on JAWTSurfaceLayers object
        JNI.callPPPV(platformInfo,
                ObjCRuntime.sel_getUid("setLayer:"),
                openglViewLayer,
                objc_msgSend);

        return view;
    }

    private static long NSOpenGLView_initWithFrame(long nsopenglView, double[] frame, long pixelFormat) {
        // Prepare the call interface
        FFICIF cif = FFICIF.malloc();

        PointerBuffer argumentTypes = BufferUtils.createPointerBuffer(7) // 4 arguments, one of them an array of 4
                // doubles
                .put(0, ffi_type_pointer) // NSOpenGLView*
                .put(1, ffi_type_pointer) // initWithFrame:pixelFormat:
                .put(2, ffi_type_double) // CGRect
                .put(3, ffi_type_double) // CGRect
                .put(4, ffi_type_double) // CGRect
                .put(5, ffi_type_double) // CGRect
                .put(6, ffi_type_pointer); // pixelFormat*

        int status = ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_pointer, argumentTypes);
        if (status != FFI_OK) {
            throw new IllegalStateException("ffi_prep_cif failed: " + status);
        }

        // An array of pointers that point to the actual argument values.
        PointerBuffer arguments = BufferUtils.createPointerBuffer(7);

        // Storage for the actual argument values.
        ByteBuffer values = BufferUtils.createByteBuffer(
                POINTER_SIZE +  // MTKView*
                        POINTER_SIZE +  // initWithFrame*
                        4 * 8 +           // CGRect
                        POINTER_SIZE    // pixelFormat*
        );

        // The memory we'll modify using libffi
        DoubleBuffer target = BufferUtils.createDoubleBuffer(4);
        target.put(frame, 0, 4);

        // Setup the argument buffers
        {
            // MTKView*
            arguments.put(memAddress(values));
            PointerBuffer.put(values, nsopenglView);

            // initWithFrame*
            arguments.put(memAddress(values));
            PointerBuffer.put(values, ObjCRuntime.sel_getUid("initWithFrame:pixelFormat:"));

            // frame
            arguments.put(memAddress(values));
            values.putDouble(frame[0]);
            arguments.put(memAddress(values));
            values.putDouble(frame[1]);
            arguments.put(memAddress(values));
            values.putDouble(frame[2]);
            arguments.put(memAddress(values));
            values.putDouble(frame[3]);

            // pixelFormat*
            arguments.put(memAddress(values));
            values.putLong(pixelFormat);
        }
        arguments.flip();
        values.flip();

        // Invoke the function and validate
        ByteBuffer view = BufferUtils.createByteBuffer(8);
        ffi_call(cif, ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend"), view, arguments);
        cif.free();

        final long v = view.asLongBuffer().get(0);
        if (v == 0L) {
            throw new IllegalStateException("[NSOpenGLView initWithFrame:pixelFormat:] returned null.");
        }

        return v;
    }

    @Override
    public boolean swapBuffers() {
        glFlush();
        return true;
    }

    @Override
    public boolean deleteContext(long context) {
        // frees created NSOpenGLView
        invokePPP(view, sel_getUid("removeFromSuperviewWithoutNeedingDisplay"), objc_msgSend);
        invokePPP(view, sel_getUid("clearGLContext"), objc_msgSend);
        invokePPP(view, sel_getUid("release"), objc_msgSend);
        return false;
    }

    @Override
    public boolean makeCurrent(long context) {
        CGLSetCurrentContext(context);
        if (context != 0L) {
            JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
            try {
                int width = dsi.bounds().width();
                int height = dsi.bounds().height();
                if (width != this.width || height != this.height) {
                    // [NSOpenGLCotext update] seems bugged. Updating renderer context with CGL works.
                    CGLSetParameter(context, kCGLCPSurfaceBackingSize, new int[]{width, height});
                    CGLEnable(context, kCGLCESurfaceBackingSize);
                    this.width = width;
                    this.height = height;
                }
            } finally {
                JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
            }
        }
        return true;
    }

    @Override
    public boolean isCurrent(long context) {
        return CGLGetCurrentContext() == context;
    }


    @Override
    public boolean delayBeforeSwapNV(float seconds) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void lock() throws AWTException {
        int lock = JAWT_DrawingSurface_Lock(ds, ds.Lock());
        if ((lock & JAWT_LOCK_ERROR) != 0)
            throw new AWTException("JAWT_DrawingSurface_Lock() failed");
    }

    @Override
    public void unlock() {
        JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
    }

    @Override
    public void dispose() {
        if (this.ds != null) {
            JAWT_FreeDrawingSurface(this.ds, awt.FreeDrawingSurface());
            this.ds = null;
        }
    }

}
