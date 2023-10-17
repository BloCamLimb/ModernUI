/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.geom;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;

public final class DefaultGeoProc extends GeometryProcessor {

    public static final int FLAG_COLOR_ATTRIBUTE = 0x1;
    public static final int FLAG_TEX_COORD_ATTRIBUTE = 0x2;

    public static final Attribute
            POSITION = new Attribute("Pos", Engine.VertexAttribType.kFloat2, SLDataType.kFloat2),
            COLOR = new Attribute("Color", Engine.VertexAttribType.kUByte4_norm, SLDataType.kFloat4),
            TEX_COORD = new Attribute("UV", Engine.VertexAttribType.kFloat2, SLDataType.kFloat2);

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

    @Nonnull
    @Override
    public String name() {
        return "Default_GeomProc";
    }

    @Override
    public byte primitiveType() {
        return 0;
    }

    @Override
    public void addToKey(KeyBuilder b) {
        b.addBits(3, mFlags, "gpFlags");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }
}
