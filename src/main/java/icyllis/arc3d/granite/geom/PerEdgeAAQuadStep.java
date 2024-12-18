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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (c) 2011 Google Inc. All rights reserved.
 *
 *   Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are
 *   met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *
 *     * Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *   OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.BufferViewInfo;
import icyllis.arc3d.engine.KeyBuilder;
import icyllis.arc3d.engine.ShaderCaps;
import icyllis.arc3d.engine.VertexInputLayout;
import icyllis.arc3d.granite.CommonDepthStencilSettings;
import icyllis.arc3d.granite.Draw;
import icyllis.arc3d.granite.GeometryStep;
import icyllis.arc3d.granite.MeshDrawWriter;
import icyllis.arc3d.granite.StaticBufferManager;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.engine.Engine.VertexAttribType;

import java.util.Formatter;

import static org.lwjgl.system.MemoryUtil.*;

// Modified from Skia src/gpu/graphite/render/PerEdgeAAQuadRenderStep.cpp
public class PerEdgeAAQuadStep extends GeometryStep {

    /*
     * Per-vertex attributes.
     */

    public static final Attribute NORMAL =
            new Attribute("Normal", VertexAttribType.kFloat2, SLDataType.kFloat2);

    /*
     * Per-instance attributes.
     */

    public static final Attribute QUAD_XS =
            new Attribute("QuadXs", VertexAttribType.kFloat4, SLDataType.kFloat4);
    public static final Attribute QUAD_YS =
            new Attribute("QuadYs", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * Bitfield: <br>
     * 16-32 bits: painter's depth; <br>
     * 0-4 bits: quad's edge flags; <br>
     */
    public static final Attribute FLAGS_AND_DEPTH =
            new Attribute("FlagsAndDepth", VertexAttribType.kUInt, SLDataType.kUInt);

    public static final AttributeSet VERTEX_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    NORMAL);
    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, QUAD_XS, QUAD_YS, FLAGS_AND_DEPTH, MODEL_VIEW);

    public static final int CORNER_COUNT = 4;
    public static final int VERTEX_COUNT = 4 * CORNER_COUNT;
    public static final int INDEX_COUNT = 29;

    @SuppressWarnings("PointlessArithmeticExpression")
    //@formatter:off
    private static short @NonNull [] getIndices() {
        final short kTL = 0 * 4;
        final short kTR = 1 * 4;
        final short kBR = 2 * 4;
        final short kBL = 3 * 4;
        return new short[]{
                // Exterior AA ramp outset
                (short) (kTL+1), (short) (kTL+2), (short) (kTL+3), (short) (kTR+0), (short) (kTR+3), (short) (kTR+1),
                (short) (kTR+1), (short) (kTR+2), (short) (kTR+3), (short) (kBR+0), (short) (kBR+3), (short) (kBR+1),
                (short) (kBR+1), (short) (kBR+2), (short) (kBR+3), (short) (kBL+0), (short) (kBL+3), (short) (kBL+1),
                (short) (kBL+1), (short) (kBL+2), (short) (kBL+3), (short) (kTL+0), (short) (kTL+3), (short) (kTL+1),
                (short) (kTL+3),
                // Fill triangles
                (short) (kTL+3), (short) (kTR+3), (short) (kBL+3), (short) (kBR+3)
        };
    }
    //@formatter:on

    private final BufferViewInfo mVertexBuffer;
    private final BufferViewInfo mIndexBuffer;

    @SuppressWarnings("PointlessArithmeticExpression")
    public PerEdgeAAQuadStep(@NonNull StaticBufferManager bufferManager) {
        super("PerEdgeAAQuadStep", "",
                VERTEX_ATTRIBS, INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA |
                FLAG_HANDLE_SOLID_COLOR,
                PrimitiveType.kTriangleStrip,
                CommonDepthStencilSettings.kDirectDepthGreaterPass
        );

        var vertexBuffer = new BufferViewInfo();
        var vertexWriter = bufferManager.getVertexWriter(4 * 2 * VERTEX_COUNT,
                vertexBuffer);
        if (vertexWriter != NULL) {
            // This template is repeated 4 times in the vertex buffer, for each of the four corners.
            // The vertex ID is used to lookup per-corner instance properties such as positions,
            // but otherwise this vertex data produces a consistent clockwise mesh from
            // TL -> TR -> BR -> BL.
            for (int i = 0; i < 4; i++) {
                // Normals for device-space AA outsets from outer curve
                memPutFloat(vertexWriter+0 , 1.0f);
                memPutFloat(vertexWriter+4 , 0.0f);
                memPutFloat(vertexWriter+8 , MathUtil.INV_SQRT2);
                memPutFloat(vertexWriter+12, MathUtil.INV_SQRT2);
                memPutFloat(vertexWriter+16, 0.0f);
                memPutFloat(vertexWriter+20, 1.0f);
                // Normal for outer anchor (zero length to signal no local or device-space normal outset)
                memPutFloat(vertexWriter+24, 0.0f);
                memPutFloat(vertexWriter+28, 0.0f);
                vertexWriter += 4 * 2 * CORNER_COUNT;
            }
        }
        mVertexBuffer = vertexBuffer;

        var indexBuffer = new BufferViewInfo();
        var indexWriter = bufferManager.getIndexWriter(2 * INDEX_COUNT,
                indexBuffer);
        if (indexWriter != NULL) {
            final short[] indices = getIndices();
            memShortBuffer(indexWriter, 2 * INDEX_COUNT)
                    .put(indices);
        }
        mIndexBuffer = indexBuffer;

        assert vertexStride() == 4 * 2;
        assert instanceStride() == 88;
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
        // Device-space distance to LTRB edges of quad.
        varyingHandler.addVarying("f_EdgeDistances", SLDataType.kFloat4);
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
                uint flags = %s;
                // LTRB
                float4 edgeAA = float4(flags&1, (flags>>1)&1, (flags>>2)&1, (flags>>3)&1);
                float4 xs = %s;
                float4 ys = %s;
                """, FLAGS_AND_DEPTH.name(), QUAD_XS.name(), QUAD_YS.name());
        // Calculate the local edge vectors, ordered L, T, R, B starting from the bottom left point.
        // For quadrilaterals these are not necessarily axis-aligned, but in all cases they orient
        // the +X/+Y normalized vertex template for each corner.
        vs.format("""
                float4 dx = xs - xs.wxyz;
                float4 dy = ys - ys.wxyz;
                float4 edgeSquaredLen = dx*dx + dy*dy;
                """);
        vs.format("""
                float4 edgeMask = sign(edgeSquaredLen); // 0 for zero-length edge, 1 for non-zero edge.
                if (any(equal(edgeMask, float4(0.0)))) {
                    // Must clean up (dx,dy) depending on the empty edge configuration
                    if (all(equal(edgeMask, float4(0.0)))) {
                        // A point so use the canonical basis
                        dx = float4( 0.0, 1.0, 0.0, -1.0);
                        dy = float4(-1.0, 0.0, 1.0,  0.0);
                        edgeSquaredLen = float4(1.0);
                    } else {
                        // Triangles (3 non-zero edges) copy the adjacent edge. Otherwise it's a line so
                        // replace empty edges with the left-hand normal vector of the adjacent edge.
                        bool triangle = (edgeMask[0] + edgeMask[1] + edgeMask[2] + edgeMask[3]) > 2.5;
                        float4 edgeX = triangle ? dx.yzwx :  dy.yzwx;
                        float4 edgeY = triangle ? dy.yzwx : -dx.yzwx;
            
                        dx = mix(edgeX, dx, edgeMask);
                        dy = mix(edgeY, dy, edgeMask);
                        edgeSquaredLen = mix(edgeSquaredLen.yzwx, edgeSquaredLen, edgeMask);
                        edgeAA = mix(edgeAA.yzwx, edgeAA, edgeMask);
                    }
                }
                """);
        vs.format("""
                float4 inverseEdgeLen = inversesqrt(edgeSquaredLen);
                dx *= inverseEdgeLen;
                dy *= inverseEdgeLen;
                """);
        // Calculate local coordinate for the vertex (relative to xAxis and yAxis at first).
        vs.format("""
                int cornerID = gl_VertexID / 4;
                float2 xAxis = -float2(dx.yzwx[cornerID], dy.yzwx[cornerID]);
                float2 yAxis =  float2(dx.xyzw[cornerID], dy.xyzw[cornerID]);
                """);
        // Vertex is outset from the base shape (and possibly with an additional AA outset later
        // in device space).
        vs.format("""
                float2 localPos = float2(xs[cornerID], ys[cornerID]);
                """);
        // Calculate edge distances and device space coordinate for the vertex
        vs.format("""
                float4 edgeDistances = dy*(xs - localPos.x) - dx*(ys - localPos.y);
                """);
        // NOTE: This 3x3 inverse is different than just taking the 1st two columns of the 4x4
        // inverse of the original SkM44 local-to-device matrix. We could calculate the 3x3 inverse
        // and upload it, but it does not seem to be a bottleneck and saves on bandwidth to
        // calculate it here instead.
        vs.format("""
                float3x3 deviceToLocal = inverse(%1$s);
                float3 devPos = %1$s * localPos.xy1;
                """, MODEL_VIEW.name());
        vs.format("""
                // Apply the Jacobian in the vertex shader so any quadrilateral normals do not have to
                // be passed to the fragment shader. However, it's important to use the Jacobian at a
                // vertex on the edge, not the current vertex's Jacobian.
                float4 gx = -dy*(deviceToLocal[0].x - deviceToLocal[0].z*xs) +
                             dx*(deviceToLocal[0].y - deviceToLocal[0].z*ys);
                float4 gy = -dy*(deviceToLocal[1].x - deviceToLocal[1].z*xs) +
                             dx*(deviceToLocal[1].y - deviceToLocal[1].z*ys);
                // NOTE: The gradient is missing a W term so edgeDistances must still be multiplied by
                // 1/w in the fragment shader. The same goes for the encoded coverage scale.
                edgeDistances *= inversesqrt(gx*gx + gy*gy);
            
                // Bias non-AA edge distances by device W so its coverage contribution is >= 1.0
                // Add additional 1/2 bias here so we don't have to do so in the fragment shader.
                edgeDistances += (1.5 - edgeAA)*abs(devPos.z);
            
                // Only outset for a vertex that is in front of the w=0 plane to avoid dealing with outset
                // triangles rasterizing differently from the main triangles as w crosses 0.
                if (any(notEqual(Normal, float2(0.0))) && devPos.z > 0.0) {
                    // Note that when there's no perspective, the jacobian is equivalent to the normal
                    // matrix (inverse transpose), but produces correct results when there's perspective
                    // because it accounts for the position's influence on a line's projected direction.
                    float2x2 J = float2x2(deviceToLocal[0].xy - deviceToLocal[0].z*localPos,
                                          deviceToLocal[1].xy - deviceToLocal[1].z*localPos);
            
                    float2 edgeAANormal = float2(edgeAA[cornerID], edgeAA.yzwx[cornerID]) * Normal;
                    float2 nx = edgeAANormal.x * float2( yAxis.y, -yAxis.x) * J;
                    float2 ny = edgeAANormal.y * float2(-xAxis.y,  xAxis.x) * J;
            
                    bool isMidVertex = all(notEqual(edgeAANormal, float2(0)));
                    if (isMidVertex) {
                        // Produce a bisecting vector in device space.
                        nx = normalize(nx);
                        ny = normalize(ny);
                        if (dot(nx, ny) < -0.8) {
                            // Normals are in nearly opposite directions, so adjust to avoid float error.
                            float s = sign(determinant(float2x2(nx, ny)));
                            nx =  s*float2(-nx.y,nx.x);
                            ny = -s*float2(-ny.y,ny.x);
                        }
                    }
                    // Adding the normal components together directly results in what we'd have
                    // calculated if we'd just transformed 'normal' in one go, assuming they weren't
                    // normalized in the if-block above. If they were normalized, the sum equals the
                    // bisector between the original nx and ny.
                    //
                    // We multiply by W so that after perspective division the new point is offset by the
                    // now-unit normal.
                    // NOTE: (nx + ny) can become the zero vector if the device outset is for an edge
                    // marked as non-AA. In this case normalize() could produce the zero vector or NaN.
                    // Until a counter-example is found, GPUs seem to discard triangles with NaN vertices,
                    // which has the same effect as outsetting by the zero vector with this mesh, so we
                    // don't bother guarding the normalize() (yet).
                    devPos.xy += devPos.z * normalize(nx + ny);
            
                    // By construction these points are 1px away from the outer edge in device space.
                    // Apply directly to edgeDistances to save work per pixel later on.
                    edgeDistances -= devPos.z;
                }
                f_EdgeDistances = edgeDistances;
                """);
        if (usesFastSolidColor) {
            // setup pass through color
            vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());
        }
        vs.format("vec4 %s = vec4(devPos.xy, float(flags >> 16u) / 65535.0, devPos.z);\n",
                worldPosVar);
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
    public void emitFragmentCoverageCode(Formatter fs, String outputCoverage) {
        // This represents a filled rectangle or quadrilateral, where the distances have already
        // been converted to device space.
        fs.format("""
                float2 outerDist = min(f_EdgeDistances.xy, f_EdgeDistances.zw);
                float c = min(outerDist.x, outerDist.y) * gl_FragCoord.w;
                %s = vec4(saturate(c));
                """, outputCoverage);
    }

    private static boolean is_clockwise(EdgeAAQuad q) {
        if (q.isRect()) {
            return true;
        }

        // This assumes that each corner has a consistent winding, which is the case for convex inputs,
        // which is an assumption of the per-edge AA API. Check the sign of cross product between the
        // first two edges.
        float winding = (q.x0 - q.x3) * (q.y1 - q.y0) - (q.y0 - q.y3) * (q.x1 - q.x0);
        if (winding == 0.f) {
            // The input possibly forms a triangle with duplicate vertices, so check the opposite corner
            winding = (q.x2 - q.x1) * (q.y3 - q.y2) - (q.y2 - q.y1) * (q.x3 - q.x2);
        }

        // At this point if winding is < 0, the quad's vertices are CCW. If it's still 0, the vertices
        // form a line, in which case the vertex shader constructs a correct CW winding. Otherwise,
        // the quad or triangle vertices produce a positive winding and are CW.
        return winding >= 0.f;
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw,
                          float @Nullable [] solidColor, boolean mayRequireLocalCoords) {
        writer.beginInstances(mVertexBuffer, mIndexBuffer, INDEX_COUNT);
        long instanceData = writer.append(1);
        if (solidColor != null) {
            memPutFloat(instanceData, solidColor[0]);
            memPutFloat(instanceData + 4, solidColor[1]);
            memPutFloat(instanceData + 8, solidColor[2]);
            memPutFloat(instanceData + 12, solidColor[3]);
        } else {
            // 0.0F is 0s
            memPutLong(instanceData, 0);
            memPutLong(instanceData + 8, 0);
        }
        var quad = (EdgeAAQuad) draw.mGeometry;
        int edgeSigns = quad.edgeFlags();

        // The vertex shader expects points to be in clockwise order. EdgeAAQuad is the only
        // shape that *might* have counter-clockwise input.
        if (is_clockwise(quad)) {
            memPutFloat(instanceData+16, quad.x0);
            memPutFloat(instanceData+20, quad.x1);
            memPutFloat(instanceData+24, quad.x2);
            memPutFloat(instanceData+28, quad.x3);
            memPutFloat(instanceData+32, quad.y0);
            memPutFloat(instanceData+36, quad.y1);
            memPutFloat(instanceData+40, quad.y2);
            memPutFloat(instanceData+44, quad.y3);
        } else {
            // swap left and right AA bits
            int tmp = (edgeSigns ^ (edgeSigns >>> 2)) & 1;
            edgeSigns ^= tmp | (tmp << 2);
            // swap TL with TR, and BL with BR
            memPutFloat(instanceData+16, quad.x1);
            memPutFloat(instanceData+20, quad.x0);
            memPutFloat(instanceData+24, quad.x3);
            memPutFloat(instanceData+28, quad.x2);
            memPutFloat(instanceData+32, quad.y1);
            memPutFloat(instanceData+36, quad.y0);
            memPutFloat(instanceData+40, quad.y3);
            memPutFloat(instanceData+44, quad.y2);
        }

        memPutInt(instanceData+48, edgeSigns | (draw.getDepth() << 16));
        draw.mTransform.store(instanceData+52);
        writer.endAppender();
    }
}
