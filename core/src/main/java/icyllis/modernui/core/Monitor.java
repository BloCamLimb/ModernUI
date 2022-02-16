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

import icyllis.modernui.annotation.MainThread;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectCollections;
import org.jetbrains.annotations.UnmodifiableView;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a currently connected Monitor (sometimes known as Display).
 * All methods must be called on the main thread and after GLFW initialization.
 * This class can only be used when the application runs independently.
 */
@MainThread
public final class Monitor {

    private static final Long2ObjectArrayMap<Monitor> sMonitors = new Long2ObjectArrayMap<>();
    private static final ObjectCollection<Monitor> sMonitorsView = ObjectCollections.unmodifiable(sMonitors.values());
    private static final CopyOnWriteArrayList<MonitorEventListener> sListeners = new CopyOnWriteArrayList<>();

    static {
        GLFW.glfwSetMonitorCallback(Monitor::onMonitorCallback);
        PointerBuffer pointers = GLFW.glfwGetMonitors();
        if (pointers != null) {
            for (int i = 0; i < pointers.limit(); ++i) {
                long p = pointers.get(i);
                sMonitors.put(p, new Monitor(p));
            }
        }
    }

    private static void onMonitorCallback(long monitor, int event) {
        if (event == GLFW.GLFW_CONNECTED) {
            Monitor mon = new Monitor(monitor);
            sMonitors.put(monitor, mon);
            for (MonitorEventListener listener : sListeners) {
                listener.onMonitorConnected(mon);
            }
        } else if (event == GLFW.GLFW_DISCONNECTED) {
            Monitor mon = sMonitors.remove(monitor);
            if (mon != null) {
                for (MonitorEventListener listener : sListeners) {
                    listener.onMonitorDisconnected(mon);
                }
            }
        }
    }

    @Nullable
    public static Monitor get(long handle) {
        return sMonitors.get(handle);
    }

    @Nullable
    public static Monitor getPrimary() {
        return sMonitors.get(GLFW.glfwGetPrimaryMonitor());
    }

    @UnmodifiableView
    public static Collection<Monitor> getAll() {
        return sMonitorsView;
    }

    public static void addMonitorEventListener(@Nonnull MonitorEventListener listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static void removeMonitorEventListener(@Nonnull MonitorEventListener listener) {
        sListeners.remove(listener);
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
     * Get the x position of this monitor in virtual screen coordinates.
     *
     * @return the x position
     */
    public int getXPos() {
        return mXPos;
    }

    /**
     * Get the y position of this monitor in virtual screen coordinates.
     *
     * @return the y position
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

    @Nonnull
    public String getName() {
        String s = GLFW.glfwGetMonitorName(mHandle);
        return s == null ? "" : s;
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

    public interface MonitorEventListener {

        void onMonitorConnected(@Nonnull Monitor monitor);

        void onMonitorDisconnected(@Nonnull Monitor monitor);
    }
}
