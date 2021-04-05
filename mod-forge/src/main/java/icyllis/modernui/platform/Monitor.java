/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.platform;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.*;

/**
 * Represents a Monitor (sometimes known as Display) that connected to
 * your operating system.
 */
public final class Monitor {

    private static final Long2ObjectMap<Monitor> sMonitors = new Long2ObjectArrayMap<>();

    static {
        GLFW.glfwSetMonitorCallback(Monitor::callbackMonitor);
        PointerBuffer pointers = GLFW.glfwGetMonitors();
        if (pointers != null) {
            for (int i = 0; i < pointers.limit(); ++i) {
                long ptr = pointers.get(i);
                sMonitors.put(ptr, new Monitor(ptr));
            }
        }
    }

    private static void callbackMonitor(long monitor, int event) {
        if (event == GLFW.GLFW_CONNECTED) {
            sMonitors.put(monitor, new Monitor(monitor));
        } else if (event == GLFW.GLFW_DISCONNECTED) {
            sMonitors.remove(monitor);
        }
    }

    @Nullable
    public static Monitor get(long monPtr) {
        return sMonitors.get(monPtr);
    }

    @Nullable
    public static Monitor getPrimary() {
        return sMonitors.get(GLFW.glfwGetPrimaryMonitor());
    }

    @Nonnull
    public static Collection<Monitor> getAll() {
        return sMonitors.values();
    }

    private final long mHandle;

    private final int mXPos;
    private final int mYPos;

    private final VideoMode[] mVideoModes;

    private Monitor(long handle) {
        mHandle = handle;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            GLFW.glfwGetMonitorPos(handle, x, y);
            mXPos = x.get(0);
            mYPos = y.get(0);
        }

        List<VideoMode> list = new ArrayList<>();
        GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(handle);
        if (buffer == null) {
            throw new IllegalStateException("Failed to get video modes");
        }
        for (int i = buffer.limit() - 1; i >= 0; --i) {
            buffer.position(i);
            VideoMode m = new VideoMode(buffer);
            if (m.getRedBits() >= 8 && m.getGreenBits() >= 8 && m.getBlueBits() >= 8) {
                list.add(m);
            }
        }
        mVideoModes = list.toArray(new VideoMode[0]);
    }

    public long getHandle() {
        return mHandle;
    }

    /**
     * Get the x-coordinate of this monitor in virtual screen coordinates.
     *
     * @return the x-coordinate
     */
    public int getXPos() {
        return mXPos;
    }

    /**
     * Get the y-coordinate of this monitor in virtual screen coordinates.
     *
     * @return the y-coordinate
     */
    public int getYPos() {
        return mYPos;
    }

    @Nonnull
    public VideoMode getCurrentMode() {
        GLFWVidMode mode = GLFW.glfwGetVideoMode(mHandle);
        if (mode == null) {
            throw new IllegalStateException("Failed to get current video mode");
        }
        return new VideoMode(mode);
    }

    @Nullable
    public String getName() {
        return GLFW.glfwGetMonitorName(mHandle);
    }

    /**
     * Return the number of supported video modes for this Monitor.
     *
     * @return the number of supported video modes
     */
    public int getModeCount() {
        return mVideoModes.length;
    }

    @Nonnull
    public VideoMode getModeAt(int index) {
        return mVideoModes[index];
    }

    @Nonnull
    public VideoMode findBestMode(int width, int height) {
        return Arrays.stream(mVideoModes)
                .filter(m -> m.getWidth() <= width && m.getHeight() <= height)
                .sorted((c1, c2) -> c2.getWidth() - c1.getWidth())
                .sorted((c1, c2) -> c2.getHeight() - c1.getHeight())
                .max(Comparator.comparingInt(VideoMode::getRefreshRate))
                .orElse(mVideoModes[0]);
    }
}
