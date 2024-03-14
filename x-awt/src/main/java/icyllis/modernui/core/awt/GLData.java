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

/**
 * Contains all information to create an OpenGL context on an {@link AWTGLCanvas}.
 *
 * @author Kai Burjack
 */
public class GLData {

    /*
     * The following fields are taken from SWT's original GLData
     */

    /**
     * Whether to use double-buffering. It defaults to <code>true</code>.
     */
    public boolean doubleBuffer = true;
    /**
     * Whether to use different LEFT and RIGHT backbuffers for stereo rendering. It defaults to <code>false</code>.
     */
    public boolean stereo;
    /**
     * The number of bits for the red color channel. It defaults to 8.
     */
    public int redSize = 8;
    /**
     * The number of bits for the green color channel. It defaults to 8.
     */
    public int greenSize = 8;
    /**
     * The number of bits for the blue color channel. It defaults to 8.
     */
    public int blueSize = 8;
    /**
     * The number of bits for the alpha color channel. It defaults to 8.
     */
    public int alphaSize = 8;
    /**
     * The number of bits for the depth channel. It defaults to 24.
     */
    public int depthSize = 24;
    /**
     * The number of bits for the stencil channel. It defaults to 0.
     */
    public int stencilSize;
    /**
     * The number of bits for the red accumulator color channel. It defaults to 0.
     */
    public int accumRedSize;
    /**
     * The number of bits for the green accumulator color channel. It defaults to 0.
     */
    public int accumGreenSize;
    /**
     * The number of bits for the blue accumulator color channel. It defaults to 0.
     */
    public int accumBlueSize;
    /**
     * The number of bits for the alpha accumulator color channel. It defaults to 0.
     */
    public int accumAlphaSize;
    /**
     * This is ignored. It will implicitly be 1 if {@link #samples} is set to a value greater than or equal to 1.
     */
    public int sampleBuffers;
    /**
     * The number of (coverage) samples for multisampling. Multisampling will only be requested for a value greater than
     * or equal to 1.
     */
    public int samples;
    /**
     * The {@link AWTGLCanvas} whose context objects should be shared with the context created using <code>this</code>
     * GLData.
     */
    public AWTGLCanvas shareContext;

    /*
     * New fields not in SWT's GLData
     */

    public enum Profile {
        CORE, COMPATIBILITY;
    }

    public enum API {
        GL, GLES;
    }

    public enum ReleaseBehavior {
        NONE, FLUSH;
    }

    /**
     * The major GL context version to use. It defaults to 0 for "not specified".
     */
    public int majorVersion;
    /**
     * The minor GL context version to use. If {@link #majorVersion} is 0 this field is unused.
     */
    public int minorVersion;
    /**
     * Whether a forward-compatible context should be created. This has only an effect when
     * ({@link #majorVersion}.{@link #minorVersion}) is at least 3.2.
     */
    public boolean forwardCompatible;
    /**
     * The profile to use. This is only valid when ({@link #majorVersion}.{@link #minorVersion}) is at least 3.0.
     */
    public Profile profile;
    /**
     * The client API to use. It defaults to {@link API#GL OpenGL for Desktop}.
     */
    public API api = API.GL;
    /**
     * Whether a debug context should be requested.
     */
    public boolean debug;
    /**
     * Set the swap interval. It defaults to <code>null</code> for "not specified".
     */
    public Integer swapInterval;
    /**
     * Whether to use sRGB color space.
     */
    public boolean sRGB;
    /**
     * Whether to use a floating point pixel format.
     */
    public boolean pixelFormatFloat;
    /**
     * Specify the behavior on context switch. Defaults to <code>null</code> for "not specified".
     */
    public ReleaseBehavior contextReleaseBehavior;
    /**
     * The number of color samples per pixel. This is only valid when {@link #samples} is at least 1.
     */
    public int colorSamplesNV;
    /**
     * The swap group index. Use this to synchronize buffer swaps across multiple windows on the same system.
     */
    public int swapGroupNV;
    /**
     * The swap barrier index. Use this to synchronize buffer swaps across multiple systems. This requires a Nvidia
     * G-Sync card.
     */
    public int swapBarrierNV;
    /**
     * Whether robust buffer access should be used.
     */
    public boolean robustness;
    /**
     * When {@link #robustness} is <code>true</code> then this specifies whether a GL_LOSE_CONTEXT_ON_RESET_ARB reset
     * notification is sent, as described by GL_ARB_robustness.
     */
    public boolean loseContextOnReset;
    /**
     * When {@link #robustness} is <code>true</code> and {@link #loseContextOnReset} is <code>true</code> then this
     * specifies whether a graphics reset only affects the current application and no other
     * application in the system.
     */
    public boolean contextResetIsolation;

}
