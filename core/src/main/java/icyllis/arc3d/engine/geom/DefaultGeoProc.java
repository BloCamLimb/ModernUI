/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine.geom;

import icyllis.arc3d.engine.*;
import icyllis.modernui.annotation.NonNull;

import static icyllis.arc3d.engine.Engine.VertexAttribType;

public final class DefaultGeoProc extends GeometryProcessor {

    public static final int FLAG_COLOR_ATTRIBUTE = 0x1;
    public static final int FLAG_TEX_COORD_ATTRIBUTE = 0x2;

    public static final Attribute
            POSITION = new Attribute("Pos", VertexAttribType.kFloat2, SLDataType.kFloat2),
            COLOR = new Attribute("Color", VertexAttribType.kUByte4_norm, SLDataType.kFloat4),
            TEX_COORD = new Attribute("UV", VertexAttribType.kFloat2, SLDataType.kFloat2);

    public static final AttributeSet VERTEX_ATTRIBS = AttributeSet.makeImplicit(
            POSITION, COLOR, TEX_COORD
    );

    private final int mFlags;

    public DefaultGeoProc(int flags) {
        super(DefaultGeoProc_ClassID);
        mFlags = flags;
        int mask = 0x1;
        if ((flags & FLAG_COLOR_ATTRIBUTE) != 0) {
            mask |= 0x2;
        }
        if ((flags & FLAG_TEX_COORD_ATTRIBUTE) != 0) {
            mask |= 0x4;
        }
        setVertexAttributes(VERTEX_ATTRIBS, mask);
    }

    @Override
    public byte primitiveType() {
        return 0;
    }

    @Override
    public void addToKey(Key.Builder b) {
        b.addBits(3, mFlags, "gpFlags");
    }

    @NonNull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @NonNull
    @Override
    public String name() {
        return "DefaultGeoProc";
    }
}
