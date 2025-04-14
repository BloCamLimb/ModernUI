/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2025 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.DepthStencilSettings;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.KeyBuilder;
import icyllis.arc3d.engine.ShaderCaps;
import icyllis.arc3d.engine.VertexInputLayout;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.granite.Draw;
import icyllis.arc3d.granite.GeometryStep;
import icyllis.arc3d.granite.MeshDrawWriter;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Formatter;

/**
 * Fills the given bounds.
 */
public class CoverBoundsStep extends GeometryStep {

    /**
     * Local rect bounds or device rect bounds (if inverted)
     */
    public static final Attribute BOUNDS =
            new Attribute("Bounds", VertexAttribType.kFloat4, SLDataType.kFloat4);

    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, BOUNDS, DEPTH, MODEL_VIEW);

    public CoverBoundsStep(@NonNull String variantName, DepthStencilSettings depthStencilSettings) {
        super("CoverBoundsStep", variantName,
                null, INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_HANDLE_SOLID_COLOR,
                PrimitiveType.kTriangleStrip,
                depthStencilSettings);
    }

    @Override
    public void appendToKey(@NonNull KeyBuilder b) {
    }

    @Override
    public @NonNull ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @Override
    public void emitVaryings(VaryingHandler varyingHandler, boolean usesFastSolidColor) {
        if (usesFastSolidColor) {
            // solid color
            varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                    VaryingHandler.kCanBeFlat_Interpolation);
        }
    }

    @Override
    public void emitUniforms(UniformHandler uniformHandler, boolean mayRequireLocalCoords) {
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @NonNull String worldPosVar,
                                   @Nullable String localPosVar, boolean usesFastSolidColor) {
        vs.format("""
                float2 corner = float2(SV_VertexID >> 1, SV_VertexID & 1);
                float4 bounds = %s;
                """, BOUNDS.name());

        vs.format("""
                float4 %1$s;
                float2 localPos;
                if (all(lessThanEqual(bounds.xy, bounds.zw))) {
                    corner = mix(bounds.xy, bounds.zw, corner);
                    float3 devicePos = %3$s * corner.xy1;
                    localPos = corner;
                    %1$s = float4(devicePos.xy, %2$s, devicePos.z);
                } else {
                    corner = mix(bounds.zw, bounds.xy, corner);
                    float3 localCoords = inverse(%3$s) * corner.xy1;
                    float invW = 1.0 / localCoords.z;
                    localPos = localCoords.xy * invW;
                    %1$s = float4(corner * invW, %2$s, invW);
                }
                """, worldPosVar, DEPTH.name(), MODEL_VIEW.name());

        if (usesFastSolidColor) {
            vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());
        }
        if (localPosVar != null) {
            vs.format("%s = localPos;\n", localPosVar);
        }
    }

    @Override
    public void emitFragmentColorCode(Formatter fs, String outputColor) {
        // setup pass through color
        fs.format("%s = %s;\n", outputColor, "f_Color");
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw,
                          float @Nullable [] solidColor,
                          boolean mayRequireLocalCoords) {
        writer.beginInstances(null, null, 4);
        long instanceData = writer.append(1);
        if (solidColor != null) {
            MemoryUtil.memPutFloat(instanceData, solidColor[0]);
            MemoryUtil.memPutFloat(instanceData + 4, solidColor[1]);
            MemoryUtil.memPutFloat(instanceData + 8, solidColor[2]);
            MemoryUtil.memPutFloat(instanceData + 12, solidColor[3]);
        } else {
            // 0.0F is 0s
            MemoryUtil.memPutLong(instanceData, 0);
            MemoryUtil.memPutLong(instanceData + 8, 0);
        }
        if (draw.mInverseFill) {
            var bounds = draw.mScissorRect;
            MemoryUtil.memPutFloat(instanceData + 16, bounds.right());
            MemoryUtil.memPutFloat(instanceData + 20, bounds.bottom());
            MemoryUtil.memPutFloat(instanceData + 24, bounds.left());
            MemoryUtil.memPutFloat(instanceData + 28, bounds.top());
        } else {
            var bounds = new Rect2f();
            draw.getBounds(bounds);
            MemoryUtil.memPutFloat(instanceData + 16, bounds.left());
            MemoryUtil.memPutFloat(instanceData + 20, bounds.top());
            MemoryUtil.memPutFloat(instanceData + 24, bounds.right());
            MemoryUtil.memPutFloat(instanceData + 28, bounds.bottom());
        }
        MemoryUtil.memPutFloat(instanceData + 32, draw.getDepthAsFloat());
        draw.mTransform.store(instanceData + 36);
        writer.endAppender();
    }
}
