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

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jawt.*;

import java.awt.*;

import static org.lwjgl.system.jawt.JAWTFunctions.*;

/**
 * Creates and manages the native AWT object with ease.
 *
 * <p>Example 1:
 * <pre>{@code
 * 	Component c = ...;
 * 	try(AWT awt = new AWT(c)) {
 * 	    long pPlatformInfo = awt.getPlatformInfo();
 * 	    // ... do something with the platform info
 *    }
 * 	// The AWT object is automatically freed!
 * }</pre>
 *
 * <p>Example 2:
 * <pre>{@code
 * 	Component c = ...;
 * 	AWT awt = new AWT(c);
 * 	long pPlatformInfo = awt.getPlatformInfo();
 * 	// ... do something with the platform info
 * 	awt.close();
 * }</pre>
 *
 * @author SWinxy
 * @author Kai Burjack
 */
public class AWT implements AutoCloseable {

    /**
     * Native JAWT object.
     * It must be freed explicitly via {@link #close()}.
     */
    private final JAWT jawt;

    /**
     * The underlying drawing surface that is used.
     */
    private final JAWTDrawingSurface drawingSurface;

    /**
     * Drawing surface metadata.
     */
    private final JAWTDrawingSurfaceInfo drawingSurfaceInfo;

    /**
     * Initializes native window handlers from the desired AWT component.
     * The component MUST be a {@link Component}, but should be a canvas
     * or window for native rendering.
     * <p>
     * If not used in a try-with-resources block, call {@link #close()} when done.
     *
     * @param component a component to render onto
     * @throws AWTException Fails for one of the provided reasons:
     *                      <ul>
     *                          <li>if the JAWT library failed to initialize;</li>
     *                          <li>if the drawing surface could not be retrieved;</li>
     *                          <li>if JAWT failed to lock the drawing surface;</li>
     *                          <li>or if JAWT failed to get information about the drawing surface;</li>
     *                      </ul>
     */
    public AWT(Component component) throws AWTException {
        jawt = JAWT
                .calloc() // MUST BE FREED
                .version(JAWT_VERSION_1_7);

        // Initialize JAWT
        if (!JAWT_GetAWT(jawt)) {
            throw new AWTException("Failed to initialize the native JAWT library.");
        }

        // Get the drawing surface from the canvas
        drawingSurface = JAWT_GetDrawingSurface(component, jawt.GetDrawingSurface());
        if (drawingSurface == null) {
            throw new AWTException("Failed to get drawing surface.");
        }

        // Try to lock the surface for native rendering
        int lock = JAWT_DrawingSurface_Lock(drawingSurface, drawingSurface.Lock());
        if ((lock & JAWT_LOCK_ERROR) != 0) {
            JAWT_FreeDrawingSurface(drawingSurface, jawt.FreeDrawingSurface());
            throw new AWTException("Failed to lock the AWT drawing surface.");
        }

        drawingSurfaceInfo = JAWT_DrawingSurface_GetDrawingSurfaceInfo(drawingSurface,
                drawingSurface.GetDrawingSurfaceInfo());
        if (drawingSurfaceInfo == null) {
            JAWT_DrawingSurface_Unlock(drawingSurface, drawingSurface.Unlock());
            throw new AWTException("Failed to get AWT drawing surface information.");
        }

        long address = drawingSurfaceInfo.platformInfo();

        if (address == MemoryUtil.NULL) {
            throw new AWTException("An unknown error occurred. Failed to retrieve platform-specific information.");
        }
    }

    /**
     * Checks if the platform is supported.
     * The OS version is not checked, but the architecture (e.g. 32 or 64 bit) is.
     *
     * @return true if the platform is supported
     */
    public static boolean isPlatformSupported() {
        return Platform.get() == Platform.WINDOWS ||
                Platform.get() == Platform.MACOSX ||
                Platform.get() == Platform.LINUX;
    }

    /**
     * Returns a pointer to a platform-specific struct with platform-specific information.
     * <p>
     * The pointer can be safely used as a {@link org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo}
     * or {@link org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo} struct, or--if on MacOS--
     * a pointer to an {@code NSObject}.
     * <p>
     * On win32 or X11 platforms, this can easily be created in Java via LWJGL's
     * {@code #create(long)} method.
     *
     * @return pointer to platform-specific data
     */
    public long getPlatformInfo() {
        return drawingSurfaceInfo.platformInfo();
    }

    /**
     * Gets the underlying platform drawing surface struct.
     *
     * @return the drawing surface
     */
    public JAWTDrawingSurfaceInfo getDrawingSurfaceInfo() {
        return drawingSurfaceInfo;
    }

    /**
     * Frees memory and unlocks the drawing surface.
     */
    @Override
    public void close() {
        // Free and unlock
        JAWT_DrawingSurface_FreeDrawingSurfaceInfo(drawingSurfaceInfo, drawingSurface.FreeDrawingSurfaceInfo());
        JAWT_DrawingSurface_Unlock(drawingSurface, drawingSurface.Unlock());
        JAWT_FreeDrawingSurface(drawingSurface, jawt.FreeDrawingSurface());

        jawt.free();
    }
}
