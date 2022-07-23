/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

/**
 * This immutable object contains information needed to build a shader program and set API
 * state for a draw. It is used along with a GeometryProcessor and a source of geometric
 * data to draw.
 */
public class Pipeline {

    // Pipeline options that the caller may enable.
    /**
     * Cause every pixel to be rasterized that is touched by the triangle anywhere (not just at
     * pixel center). Additionally, if using MSAA, the sample mask will always have 100%
     * coverage.
     * NOTE: The primitive type must be a triangle type.
     */
    public static final byte CONSERVATIVE_RASTER_FLAG = 0x01;
    /**
     * Draws triangles as outlines.
     */
    public static final byte WIREFRAME_FLAG = 0x02;
    /**
     * Modifies the vertex shader so that vertices will be positioned at pixel centers.
     */
    public static final byte SNAP_VERTICES_TO_PIXEL_CENTERS_FLAG = 0x04;

    private static final byte LAST_INPUT_FLAG = SNAP_VERTICES_TO_PIXEL_CENTERS_FLAG;

    // This is a continuation of the public "InputFlags" enum.
    private static final byte
            HAS_STENCIL_CLIP_FLAG = (LAST_INPUT_FLAG << 1),
            SCISSOR_TEST_ENABLED_FLAG = (LAST_INPUT_FLAG << 2);

    private DstProxyView mDstProxy;
    //TODO private WindowRectsState mWindowRectsState;
    private byte mFlags;

    private short mWriteSwizzle;
}
