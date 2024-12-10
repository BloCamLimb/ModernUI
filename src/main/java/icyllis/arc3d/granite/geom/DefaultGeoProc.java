/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.GeometryStep;
import org.jspecify.annotations.NonNull;

public final class DefaultGeoProc extends GeometryStep {

    public static final int FLAG_COLOR_ATTRIBUTE = 0x1;
    public static final int FLAG_TEX_COORD_ATTRIBUTE = 0x2;

    public static final VertexInputLayout.Attribute
            POSITION = new VertexInputLayout.Attribute("Pos", Engine.VertexAttribType.kFloat2, SLDataType.kFloat2),
            COLOR = new VertexInputLayout.Attribute("Color", Engine.VertexAttribType.kUByte4_norm, SLDataType.kFloat4),
            TEX_COORD = new VertexInputLayout.Attribute("UV", Engine.VertexAttribType.kFloat2, SLDataType.kFloat2);

    public static final VertexInputLayout.AttributeSet VERTEX_ATTRIBS = VertexInputLayout.AttributeSet.makeImplicit(
            0, POSITION, COLOR, TEX_COORD
    );

    private final int mFlags;

    public DefaultGeoProc(int flags) {
        super("DefaultGeoProc", "", VERTEX_ATTRIBS, null, 0, (byte) 0, null);
        mFlags = flags;
        int mask = 0x1;
        if ((flags & FLAG_COLOR_ATTRIBUTE) != 0) {
            mask |= 0x2;
        }
        if ((flags & FLAG_TEX_COORD_ATTRIBUTE) != 0) {
            mask |= 0x4;
        }
        //setVertexAttributes(mask);
    }

    @Override
    public void appendToKey(@NonNull KeyBuilder b) {
        b.addBits(3, mFlags, "gpFlags");
    }

    @NonNull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        //TODO
        return null;
    }
}
