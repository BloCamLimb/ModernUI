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

import org.lwjgl.glfw.GLFWVidMode;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * A Video Mode (aka Display Mode) of a Monitor.
 */
public final class VideoMode {

    private final int mWidth;
    private final int mHeight;
    private final int mRedBits;
    private final int mGreenBits;
    private final int mBlueBits;
    private final int mRefreshRate;

    public VideoMode(@Nonnull GLFWVidMode gLFWVidMode) {
        mWidth = gLFWVidMode.width();
        mHeight = gLFWVidMode.height();
        mRedBits = gLFWVidMode.redBits();
        mGreenBits = gLFWVidMode.greenBits();
        mBlueBits = gLFWVidMode.blueBits();
        mRefreshRate = gLFWVidMode.refreshRate();
    }

    public VideoMode(@Nonnull GLFWVidMode.Buffer buffer) {
        mWidth = buffer.width();
        mHeight = buffer.height();
        mRedBits = buffer.redBits();
        mGreenBits = buffer.greenBits();
        mBlueBits = buffer.blueBits();
        mRefreshRate = buffer.refreshRate();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getRedBits() {
        return mRedBits;
    }

    public int getGreenBits() {
        return mGreenBits;
    }

    public int getBlueBits() {
        return mBlueBits;
    }

    public int getRefreshRate() {
        return mRefreshRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoMode videoMode = (VideoMode) o;
        return mWidth == videoMode.mWidth && mHeight == videoMode.mHeight && mRedBits == videoMode.mRedBits && mGreenBits == videoMode.mGreenBits && mBlueBits == videoMode.mBlueBits && mRefreshRate == videoMode.mRefreshRate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mWidth, mHeight, mRedBits, mGreenBits, mBlueBits, mRefreshRate);
    }

    @Override
    public String toString() {
        int colorBitDepth = mRedBits + mGreenBits + mBlueBits;
        return String.format("VideoMode: %dx%d@%dHz (%d bits)", mWidth, mHeight, mRefreshRate, colorBitDepth);
    }
}
