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

package icyllis.arcui.hgi;

public class SurfaceProxyView {

    public static final int FLAG_ORIGIN_TOP_LEFT = 0x0;
    public static final int FLAG_ORIGIN_BOTTOM_LEFT = 0x1;
    public static final int FLAG_REQUIRES_TEXTURE_BARRIER = 0x2;
    public static final int FLAG_AS_INPUT_ATTACHMENT = 0x4;

    private SurfaceProxy mProxy;
    private short mSwizzle;
    private int mOffsetX;
    private int mOffsetY;
    private int mFlags;
}
