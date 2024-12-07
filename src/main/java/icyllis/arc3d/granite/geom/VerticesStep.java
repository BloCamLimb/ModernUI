/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.core.Vertices;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Formatter;

public class VerticesStep extends GeometryStep {

    public static final Attribute POSITION =
            new Attribute("Pos", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final Attribute COLOR =
            new Attribute("Color", VertexAttribType.kUByte4_norm, SLDataType.kFloat4);
    public static final Attribute TEX_COORD =
            new Attribute("UV", VertexAttribType.kFloat2, SLDataType.kFloat2);

    public static final AttributeSet ATTRIBS_POS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    POSITION);
    public static final AttributeSet ATTRIBS_POS_COLOR =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    POSITION, COLOR);
    public static final AttributeSet ATTRIBS_POS_UV =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    POSITION, TEX_COORD);
    public static final AttributeSet ATTRIBS_POS_COLOR_UV =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    POSITION, COLOR, TEX_COORD);

    private final boolean mHasColor;
    private final boolean mHasTexCoords;

    public VerticesStep(byte primitiveType,
                        boolean hasColor, boolean hasTexCoords) {
        super("VerticesStep",
                variant_name(primitiveType, hasColor, hasTexCoords),
                hasTexCoords
                        ? (hasColor ? ATTRIBS_POS_COLOR_UV : ATTRIBS_POS_UV)
                        : (hasColor ? ATTRIBS_POS_COLOR : ATTRIBS_POS),
                null,
                hasColor
                        ? (FLAG_PERFORM_SHADING | FLAG_EMIT_PRIMITIVE_COLOR)
                        : FLAG_PERFORM_SHADING,
                primitiveType,
                CommonDepthStencilSettings.kDirectDepthGEqualPass
        );
        mHasColor = hasColor;
        mHasTexCoords = hasTexCoords;
    }

    private static String variant_name(byte primitiveType, boolean hasColor, boolean hasTexCoords) {
        String name = switch (primitiveType) {
            case PrimitiveType.kPointList -> "pts";
            case PrimitiveType.kLineList -> "lines";
            case PrimitiveType.kLineStrip -> "linestrip";
            case PrimitiveType.kTriangleList -> "tris";
            case PrimitiveType.kTriangleStrip -> "tristrip";
            default -> throw new AssertionError();
        };
        if (hasColor) {
            name += "-color";
        }
        if (hasTexCoords) {
            name += "-tex";
        }
        return name;
    }

    @Override
    public void appendToKey(@NonNull KeyBuilder b) {

    }

    @NonNull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @Override
    public void emitVaryings(VaryingHandler varyingHandler,
                             boolean usesFastSolidColor) {
        assert !usesFastSolidColor;
        // vertex color
        if (mHasColor) {
            varyingHandler.addVarying("f_Color", SLDataType.kFloat4);
        }
    }

    @Override
    public void emitUniforms(UniformHandler uniformHandler,
                             boolean mayRequireLocalCoords) {
        uniformHandler.addUniform(Engine.ShaderFlags.kVertex,
                SLDataType.kFloat3x3, "u_LocalToDevice", -1);
        uniformHandler.addUniform(Engine.ShaderFlags.kVertex,
                SLDataType.kFloat, "u_Depth", -1);
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @NonNull String worldPosVar,
                                   @Nullable String localPosVar,
                                   boolean usesFastSolidColor) {
        assert !usesFastSolidColor;
        if (mHasColor) {
            vs.format("""
                    %1$s = vec4(%2$s.rgb * %2$s.a, %2$s.a);
                    """, "f_Color", COLOR.name());
        }
        vs.format("vec3 devicePos = %s * vec3(%s, 1.0);\n",
                "u_LocalToDevice", POSITION.name());
        vs.format("vec4 %s = vec4(devicePos.xy, %s, devicePos.z);\n",
                worldPosVar, "u_Depth");
        if (localPosVar != null) {
            vs.format("%s = %s;\n",
                    localPosVar, mHasTexCoords ? TEX_COORD.name() : POSITION.name());
        }
    }

    @Override
    public void emitFragmentColorCode(Formatter fs, String outputColor) {
        fs.format("%s = %s;\n", outputColor, "f_Color");
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw,
            float @Nullable[] solidColor,
                          boolean mayRequireLocalCoords) {
        assert solidColor == null;
        Vertices vertices = (Vertices) draw.mGeometry;
        int vertexCount = vertices.getVertexCount();
        int indexCount = vertices.getIndexCount();
        float[] positions = vertices.getPositions();
        float[] texCoords = vertices.getTexCoords();
        byte[] colors = vertices.getColors();
        short[] indices = vertices.getIndices();

        assert (mHasColor == (colors != null));
        assert (mHasTexCoords == (texCoords != null));

        //TODO currently we don't have separate vertex streams and an actual index buffer
        writer.beginVertices();
        if (indices != null) {
            long vertexData = writer.append(indexCount);
            for (int i = 0; i < indexCount; i++) {
                int index = indices[i] & 0xFFFF;
                MemoryUtil.memPutFloat(vertexData, positions[index<<1]);
                MemoryUtil.memPutFloat(vertexData+4, positions[(index<<1)|1]);
                vertexData += 8;
                if (mHasColor) {
                    MemoryUtil.memPutByte(vertexData, colors[index<<2]);
                    MemoryUtil.memPutByte(vertexData+1, colors[(index<<2)|1]);
                    MemoryUtil.memPutByte(vertexData+2, colors[(index<<2)|2]);
                    MemoryUtil.memPutByte(vertexData+3, colors[(index<<2)|3]);
                    vertexData += 4;
                }
                if (mHasTexCoords) {
                    MemoryUtil.memPutFloat(vertexData, texCoords[index<<1]);
                    MemoryUtil.memPutFloat(vertexData+4, texCoords[(index<<1)|1]);
                    vertexData += 8;
                }
            }
        } else {
            long vertexData =writer.append(vertexCount);
            for (int i = 0; i < vertexCount; i++) {
                MemoryUtil.memPutFloat(vertexData, positions[i<<1]);
                MemoryUtil.memPutFloat(vertexData+4, positions[(i<<1)|1]);
                vertexData += 8;
                if (mHasColor) {
                    MemoryUtil.memPutByte(vertexData, colors[i<<2]);
                    MemoryUtil.memPutByte(vertexData+1, colors[(i<<2)|1]);
                    MemoryUtil.memPutByte(vertexData+2, colors[(i<<2)|2]);
                    MemoryUtil.memPutByte(vertexData+3, colors[(i<<2)|3]);
                    vertexData += 4;
                }
                if (mHasTexCoords) {
                    MemoryUtil.memPutFloat(vertexData, texCoords[i<<1]);
                    MemoryUtil.memPutFloat(vertexData+4, texCoords[(i<<1)|1]);
                    vertexData += 8;
                }
            }
        }
        writer.endAppender();
    }

    @Override
    public void writeUniformsAndTextures(RecordingContext context, Draw draw,
                                         UniformDataGatherer uniformDataGatherer,
                                         TextureDataGatherer textureDataGatherer,
                                         boolean mayRequireLocalCoords) {
        uniformDataGatherer.writeMatrix3f(draw.mTransform); // LocalToDevice
        uniformDataGatherer.write1f(draw.getDepthAsFloat());
    }
}
