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
import icyllis.arc3d.core.Paint;
import icyllis.arc3d.core.Point;
import icyllis.arc3d.core.RRect;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.BufferViewInfo;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.KeyBuilder;
import icyllis.arc3d.engine.ShaderCaps;
import icyllis.arc3d.engine.VertexInputLayout;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.granite.CommonDepthStencilSettings;
import icyllis.arc3d.granite.Draw;
import icyllis.arc3d.granite.GeometryStep;
import icyllis.arc3d.granite.MeshDrawWriter;
import icyllis.arc3d.granite.SimpleShape;
import icyllis.arc3d.granite.StaticBufferManager;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Formatter;

import static org.lwjgl.system.MemoryUtil.*;

// Modified from Skia src/gpu/graphite/render/AnalyticRRectRenderStep.cpp
public class AnalyticComplexBoxStep extends GeometryStep {

    /*
     * Per-vertex attributes.
     */
    public static final Attribute POSITION =
            new Attribute("Position", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final Attribute NORMAL =
            new Attribute("Normal", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final Attribute NORMAL_SCALE =
            new Attribute("NormalScale", VertexAttribType.kFloat, SLDataType.kFloat);
    public static final Attribute CENTER_WEIGHT =
            new Attribute("CenterWeight", VertexAttribType.kFloat, SLDataType.kFloat);

    /*
     * Per-instance attributes.
     */

    // if any components is > 0, the instance represents a filled round rect
    // with elliptical corners and these values specify the X radii in top-left CW order.
    // Otherwise, if .x < -1, the instance represents a stroked or hairline [round] rect or line,
    // where .y differentiates hairline vs. stroke. If .y is negative, then it is a hairline [round]
    // rect and xRadiiOrFlags stores (-2 - X radii); if .y is zero, it is a regular stroked [round]
    // rect; if .y is positive, then it is a stroked *or* hairline line. For .y >= 0, .z holds the
    // stroke radius and .w stores the join limit (matching StrokeStyle's conventions).
    // Lastly, if -1 <= .x <= 0, it's a filled quadrilateral with per-edge AA defined by each by the
    // component: aa != 0.
    public static final Attribute X_RADII_OR_FLAGS =
            new Attribute("XRadiiOrFlags", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // if in filled round rect or hairline [round] rect mode, these values
    // provide the Y radii in top-left CW order. If in stroked [round] rect mode, these values
    // provide the circular corner radii (same order). Otherwise, when in per-edge quad mode, these
    // values provide the X coordinates of the quadrilateral (same order).
    public static final Attribute RADII_OR_QUAD_XS =
            new Attribute("RadiiOrQuadXs", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // if in filled round rect mode or stroked [round] rect mode, these values
    // define the LTRB edge coordinates of the rectangle surrounding the round rect (or the
    // rect itself when the radii are 0s). In stroked line mode, LTRB is treated as (x0,y0) and
    // (x1,y1) that defines the line. Otherwise, in per-edge quad mode, these values provide
    // the Y coordinates of the quadrilateral.
    public static final Attribute LTRB_OR_QUAD_YS =
            new Attribute("LtrbOrQuadYs", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // XY stores center of rrect in local coords. Z and W store values to
    // control interior fill behavior. Z can be -1, 0, or 1:
    //   -1: A stroked interior where AA insets overlap, but isn't solid.
    //    0: A stroked interior with no complications.
    //    1: A solid interior (fill or sufficiently large stroke width).
    // W specifies the size of the AA inset if it's >= 0, or signals that
    // the inner curves intersect in a complex manner (rare).
    public static final Attribute CENTER =
            new Attribute("Center", VertexAttribType.kFloat4, SLDataType.kFloat4);

    public static final AttributeSet VERTEX_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_VERTEX,
                    POSITION, NORMAL, NORMAL_SCALE, CENTER_WEIGHT);
    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, X_RADII_OR_FLAGS, RADII_OR_QUAD_XS, LTRB_OR_QUAD_YS, CENTER, DEPTH, MODEL_VIEW);

    // Allowed values for the center weight instance value (selected at record time based on style
    // and transform), and are defined such that when (insance-weight > vertex-weight) is true, the
    // vertex should be snapped to the center instead of its regular calculation.
    private static final float
            kSolidInterior = 1.f,
            kStrokeInterior = 0.f,
            kFilledStrokeInterior = -1.f;

    // Special value for local AA radius to signal when the self-intersections of a stroke interior
    // need extra calculations in the vertex shader.
    private static final float kComplexAAInsets = -1.f;

    public static final int
            kCornerVertexCount = 9,
            kVertexCount = 4 * kCornerVertexCount,
            kIndexCount = 69;

    @SuppressWarnings("PointlessArithmeticExpression")
    //@formatter:off
    private static short[] get_indices() {
        final short kTL = 0 * kCornerVertexCount;
        final short kTR = 1 * kCornerVertexCount;
        final short kBR = 2 * kCornerVertexCount;
        final short kBL = 3 * kCornerVertexCount;
        return new short[]{
                // Exterior AA ramp outset
                kTL+0,kTL+4,kTL+1,kTL+5,kTL+2,kTL+3,kTL+5,
                kTR+0,kTR+4,kTR+1,kTR+5,kTR+2,kTR+3,kTR+5,
                kBR+0,kBR+4,kBR+1,kBR+5,kBR+2,kBR+3,kBR+5,
                kBL+0,kBL+4,kBL+1,kBL+5,kBL+2,kBL+3,kBL+5,
                kTL+0,kTL+4, // close and jump to next strip
                // Outer to inner edges
                kTL+4,kTL+6,kTL+5,kTL+7,
                kTR+4,kTR+6,kTR+5,kTR+7,
                kBR+4,kBR+6,kBR+5,kBR+7,
                kBL+4,kBL+6,kBL+5,kBL+7,
                kTL+4,kTL+6, // close and jump to next strip
                // Fill triangles
                kTL+6,kTL+8,kTL+7, kTL+7,kTR+8,
                kTR+6,kTR+8,kTR+7, kTR+7,kBR+8,
                kBR+6,kBR+8,kBR+7, kBR+7,kBL+8,
                kBL+6,kBL+8,kBL+7, kBL+7,kTL+8,
                kTL+6 // close
        };
    }
    //@formatter:on

    //@formatter:off
    private static float[] get_vertex_corner_template() {
        // Allowed values for the normal scale attribute. +1 signals a device-space outset along the
        // normal away from the outer edge of the stroke. 0 signals no outset, but placed on the outer
        // edge of the stroke. -1 signals a local inset along the normal from the inner edge.
        final float kOutset = 1.0f;
        final float kInset  = -1.0f;

        final float kCenter = 1.f; // "true" as a float

        // Zero, but named this way to help call out non-zero parameters.
        final float _______ = 0.f;

        final float kHR2 = 0.5f * MathUtil.SQRT2; // "half root 2"

        // This template is repeated 4 times in the vertex buffer, for each of the four corners.
        // The vertex ID is used to lookup per-corner instance properties such as corner radii or
        // positions, but otherwise this vertex data produces a consistent clockwise mesh from
        // TL -> TR -> BR -> BL.
        return new float[]{
                // Device-space AA outsets from outer curve
                1.0f, 0.0f,  1.0f, 0.0f,  kOutset,  _______,
                1.0f, 0.0f,  kHR2, kHR2,  kOutset,  _______,
                0.0f, 1.0f,  kHR2, kHR2,  kOutset,  _______,
                0.0f, 1.0f,  0.0f, 1.0f,  kOutset,  _______,

                // Outer anchors (no local or device-space normal outset)
                1.0f, 0.0f,  kHR2, kHR2,  _______,  _______,
                0.0f, 1.0f,  kHR2, kHR2,  _______,  _______,

                // Inner curve (with additional AA inset in the common case)
                1.0f, 0.0f,  1.0f, 0.0f,  kInset,   _______,
                0.0f, 1.0f,  0.0f, 1.0f,  kInset,   _______,

                // Center filling vertices (equal to inner AA insets unless 'center' triggers a fill).
                // TODO: On backends that support "cull" distances (and with SkSL support), these vertices
                // and their corresponding triangles can be completely removed. The inset vertices can
                // set their cull distance value to cause all filling triangles to be discarded or not
                // depending on the instance's style.
                1.0f, 0.0f,  1.0f, 0.0f,  kInset,   kCenter,
        };
    }
    //@formatter:on

    private final BufferViewInfo mVertexBuffer;
    private final BufferViewInfo mIndexBuffer;

    public AnalyticComplexBoxStep(StaticBufferManager bufferManager) {
        super("AnalyticComplexBoxStep", "",
                VERTEX_ATTRIBS, INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA
                        | FLAG_HANDLE_SOLID_COLOR,
                PrimitiveType.kTriangleStrip,
                CommonDepthStencilSettings.kDirectDepthGreaterPass);

        var vertexBuffer = new BufferViewInfo();
        var vertexWriter = bufferManager.getVertexWriter(4 * 6 * kVertexCount,
                vertexBuffer);
        if (vertexWriter != NULL) {
            final float[] template = get_vertex_corner_template();
            final var dst = memFloatBuffer(vertexWriter, 4 * 6 * kVertexCount);
            for (int i = 0; i < 4; i++) {
                dst.put(template);
            }
        }
        mVertexBuffer = vertexBuffer;

        var indexBuffer = new BufferViewInfo();
        var indexWriter = bufferManager.getIndexWriter(2 * kIndexCount,
                indexBuffer);
        if (indexWriter != NULL) {
            final short[] indices = get_indices();
            memShortBuffer(indexWriter, 2 * kIndexCount)
                    .put(indices);
        }
        mIndexBuffer = indexBuffer;

        assert vertexStride() == 4 * 6;
        assert instanceStride() == 120;
    }

    @Override
    public void appendToKey(@NonNull KeyBuilder b) {
    }

    @Override
    public @NonNull ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @Override
    public void emitVaryings(VaryingHandler varyingHandler,
                             boolean usesFastSolidColor) {
        varyingHandler.addVarying("f_Jacobian", SLDataType.kFloat4); // float2x2
        // Distance to LTRB edges of unstroked shape. Depending on
        // 'perPixelControl' these will either be local or device-space values.
        varyingHandler.addVarying("f_EdgeDistances", SLDataType.kFloat4);
        varyingHandler.addVarying("f_XRadii",
                SLDataType.kFloat4, VaryingHandler.kCanBeFlat_Interpolation);
        varyingHandler.addVarying("f_YRadii",
                SLDataType.kFloat4, VaryingHandler.kCanBeFlat_Interpolation);
        varyingHandler.addVarying("f_StrokeParams",
                SLDataType.kFloat2);
        // 'perPixelControl' is a tightly packed description of how to
        // evaluate the possible edges that influence coverage in a pixel.
        // The decision points and encoded values are spread across X and Y
        // so that they are consistent regardless of whether or not MSAA is
        // used and does not require centroid sampling.
        //
        // The signs of values are used to determine the type of coverage to
        // calculate in the fragment shader and depending on the state, extra
        // varying state is encoded in the fields:
        //  - A positive X value overrides all per-pixel coverage calculations
        //    and sets the pixel to full coverage. Y is ignored in this case.
        //  - A zero X value represents a solid interior shape.
        //  - X much less than 0 represents bidirectional coverage for a
        //    stroke, using a sufficiently negative value to avoid
        //    extrapolation from fill triangles. For actual shapes with
        //    bidirectional coverage, the fill triangles are zero area.
        //
        //  - Y much greater than 0 takes precedence over the latter two X
        //    rules and signals that 'edgeDistances' holds device-space values
        //    and does not require additional per-pixel calculations. The
        //    coverage scale is encoded as (1+scale*w) and the bias is
        //    reconstructed from that. X is always 0 for non-fill triangles
        //    since device-space edge distance is only used for solid interiors
        //  - Otherwise, any negative Y value represents an additional
        //    reduction in coverage due to a device-space outset. It is clamped
        //    below 0 to avoid adding coverage from extrapolation.
        varyingHandler.addVarying("f_PerPixelControl",
                SLDataType.kFloat2);
        if (usesFastSolidColor) {
            // solid color
            varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                    VaryingHandler.kCanBeFlat_Interpolation);
        }
    }

    @Override
    public void emitVertexDefinitions(Formatter vs) {
        vs.format("""
                float cross_length_2d(float2 a, float2 b) {
                    return determinant(float2x2(a, b));
                }
                float2 perp(float2 v) {
                    return float2(-v.y, v.x);
                }
                float4 analytic_rrect_vertex_fn(float2 a,float2 b,float c,float d
                ,float4 e,float4 f,float4 g,float4 h,float i,float3x3 j,out float4 k,out float4
                 l,out float4 m,out float4 n,out float2 o,out float2 p,out float2 q){float w
                =1.;bool x=h.z<=0.;bool y=false;float4 z;float4 A;float4 B=1..xxxx;bool C=false
                ;if(e.x<-1.){C=e.y>0.;z=C?g.xxzz:g.xzzx;A=g.yyww;if(e.y<0.){m=-e-2.;n=f;o=float2
                (0.,1.);}else{m=f;n=m;o=e.zw;w=o.y<0.?.414213568:sign(o.y);}}else if(any(greaterThan
                (e,0..xxxx))){z=g.xzzx;A=g.yyww;m=e;n=f;o=float2(0.,-1.);}else{z=f;A=g;B=-e
                ;m=0..xxxx;n=0..xxxx;o=float2(0.,1.);y=true;}uint D=uint(SV_VertexID)/%d;float2
                 E=float2(m[D],n[D]);if(D%%2!=0)E=E.yx;float2 F=1..xx;if(all(greaterThan(E,0.
                .xx))){w=.414213568;F=E.yx;}float4 G=z-z.wxyz;float4 H=A-A.wxyz;float4 I=G*
                G+H*H;float4 J=sign(I);float4 K=0..xxxx;float2 L=o.x.xx;if(any(equal(J,0..xxxx
                )))if(all(equal(J,0..xxxx))){G=float4(0.,1.,0.,-1.);H=float4(-1.,0.,1.,0.);
                I=1..xxxx;}else{bool M=((J.x+J.y)+J.z)+J.w>2.5;float4 N=M?G.yzwx:H.yzwx;float4
                 O=M?H.yzwx:-G.yzwx;G=mix(N,G,J);H=mix(O,H,J);I=mix(I.yzwx,I,J);B=mix(B.yzwx
                ,B,J);if(!M&&w==0.){L*=float2(J[D],J.yzwx[D]);K=(J-1.)*o.x;o.y=1.;w=1.;}}float4
                 M=inversesqrt(I);G*=M;H*=M;float2 N=-float2(G.yzwx[D],H.yzwx[D]);float2 O=
                float2(G[D],H[D]);float2 P;bool Q=false;if(c<0.)if(h.w<0.||d*h.z!=0.)Q=true
                ;else{float R=h.w;float2 S=E+(x?-L:L);if(w==1.||any(lessThanEqual(S,R.xx)))
                P=S-R;else P=S*a-R*b;}else P=(E+L)*(a+w*a.yx);if(Q)P=h.xy;else{P-=E;P=(float2
                (z[D],A[D])+N*P.x)+O*P.y;}l=(H*(z-P.x)-G*(A-P.y))+K;float3x3 R=inverse(j);float3
                 S=j*float3(P,1.);k=float4(R[0].xy-R[0].z*P,R[1].xy-R[1].z*P);if(y){float4 T
                =-H*(R[0].x-R[0].z*z)+G*(R[0].y-R[0].z*A);float4 U=-H*(R[1].x-R[1].z*z)+G*(
                R[1].y-R[1].z*A);l*=inversesqrt(T*T+U*U);l+=(1.-B)*abs(S.z);bool V=B==1..xxxx
                &&dot(abs(G*G.yzwx+H*H.yzwx),1..xxxx)<.00024;if(V){float2 W=l.xy+l.zw;p.y=1.
                +min(min(W.x,W.y),abs(S.z));}else p.y=1.+abs(S.z);}if(c>0.&&S.z>0.){float2x2
                 T=float2x2(k);float2 U=float2(B[D],B.yzwx[D])*b;float2 V=((F.x*U.x)*perp(-
                O))*T;float2 W=((F.y*U.y)*perp(N))*T;bool X=all(notEqual(U,0..xx));if(w==1.
                &&X){V=normalize(V);W=normalize(W);if(dot(V,W)<-.8){float Y=sign(cross_length_2d
                (V,W));V=Y*perp(V);W=-Y*perp(W);}}S.xy+=S.z*normalize(V+W);if(y)l-=S.z;else
                 p.y=-S.z;}else if(!y)p.y=0.;p.x=float(d!=0.?1.:(x?-1.:0.));if(C)k=float4(float2x2
                (H.x,-H.y,-G.x,G.y)*float2x2(k));q=P;return float4(S.xy,i,S.z);}
                """, kCornerVertexCount);
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @NonNull String worldPosVar,
                                   @Nullable String localPosVar,
                                   boolean usesFastSolidColor) {
        vs.format("""
                        float2 localPos;
                        float4 %s = analytic_rrect_vertex_fn(
                            %s,%s,%s,%s,
                            %s,%s,%s,%s,%s,%s,
                            f_Jacobian,f_EdgeDistances,f_XRadii,f_YRadii,
                            f_StrokeParams,f_PerPixelControl,localPos);
                        """,
                worldPosVar, POSITION.name(), NORMAL.name(), NORMAL_SCALE.name(), CENTER_WEIGHT.name(),
                X_RADII_OR_FLAGS.name(), RADII_OR_QUAD_XS.name(), LTRB_OR_QUAD_YS.name(), CENTER.name(),
                DEPTH.name(), MODEL_VIEW.name());
        if (usesFastSolidColor) {
            // setup pass through color
            vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());
        }
        if (localPosVar != null) {
            vs.format("%s = localPos;\n", localPosVar);
        }
    }

    @Override
    public void emitFragmentDefinitions(Formatter fs) {
        fs.format("""
                float coverage_bias(float a){return 1.-.5*a;}
                
                float _E(float2 a,float2x2 b){float2 c=a*b;return inversesqrt(dot(c,c));}
                
                float2 _F(float2 a,float2 b,float c,float2x2 d){float2 e=1./(b*b+c*c);
                float2 g=e*a;float h=_E(g,d);float i=(.5*h)*(dot(a,g)-1.);float j=((b.x*c)*e.x)*h;
                return float2(j-i,j+i);}
                
                void _G(inout float2 a,float2x2 b,float2 c,float2 d,float2 e,float2 f){
                float2 g=f-d;if(all(greaterThan(g,0..xx)))if(all(greaterThan(f,0..xx))||
                c.x>0.&&c.y<0.){float2 h=_F(g*e,f,c.x,b);h.y=f.x-c.x<=0.?1.:-h.y;a=min(a,h)
                ;}else if(c.y==0.){float h=((c.x-g.x)-g.y)*_E(e,b);a.x=min(a.x,h);}}
                
                void _H(inout float2 a,float2x2 b,float2 c,float4 e,float4 f,float4 g){_G(a,b,c,e.
                xy,-1..xx,float2(f.x,g.x));_G(a,b,c,e.zy,float2(1.,-1.),float2(f.y,g.y));_G
                (a,b,c,e.zw,1..xx,float2(f.z,g.z));_G(a,b,c,e.xw,float2(-1.,1.),float2(f.w,
                g.w));}
                
                float4 analytic_rrect_coverage_fn(float4 a,float4 b,float4 c,float4
                 d,float4 e,float2 f,float2 g){if(g.x>0.)return float4(1.);else if(g.y>1.){float2
                 h=min(c.xy,c.zw);float i=min(h.x,h.y)*a.w;float j=(g.y-1.)*a.w;float k=coverage_bias
                (j);return float4(saturate(j*(i+k)));}else{float2x2 h=float2x2(b)*(1./a.
                w);float2 i=float2(_E(float2(1.,0.),h),_E(float2(0.,1.),h));float2 j=i*(f.x
                +min(c.xy,c.zw));float2 k=float2(min(j.x,j.y),-1.);float l;float m;if(g.x>-
                .95){float2 n=i*((c.xy+c.zw)+2.*f.xx);l=min(min(n.x,n.y),1.);m=coverage_bias
                (l);}else{float2 n=(2.*f.x)*i;float2 o=n-j;k.y=-max(o.x,o.y);if(f.x>0.){float
                 p=min(n.x,n.y);float2 q=mix(p.xx,n,greaterThanEqual(o,-.5.xx));l=saturate(
                max(q.x,q.y));m=coverage_bias(l);}else l=(m=1.);}_H(k,h,f,c,d,e);float n=min
                (g.y,0.)*a.w;float o=l*(min(k.x+n,-k.y)+m);return float4(saturate(o));}}
                
                """);

    }

    @Override
    public void emitFragmentColorCode(Formatter fs, String outputColor) {
        fs.format("%s = f_Color;\n", outputColor);
    }

    @Override
    public void emitFragmentCoverageCode(Formatter fs, String outputCoverage) {
        fs.format("""
                %s = analytic_rrect_coverage_fn(SV_FragCoord, f_Jacobian,
                                                f_EdgeDistances,
                                                f_XRadii, f_YRadii,
                                                f_StrokeParams, f_PerPixelControl);
                """, outputCoverage);
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw,
                          float @Nullable [] solidColor,
                          boolean mayRequireLocalCoords) {
        writer.beginInstances(mVertexBuffer, mIndexBuffer, kIndexCount);
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

        SimpleShape shape = (SimpleShape) draw.mGeometry;

        float aaRadius = draw.mAARadius;
        float strokeInset = 0.f;
        float centerWeight = kSolidInterior;

        if (draw.isStroke()) {
            boolean isLine = shape.getType() >= SimpleShape.kLine_Type;

            float strokeRadius = draw.mHalfWidth;

            float sizeX, sizeY;
            if (isLine) {
                sizeX = Point.distanceTo(shape.left(), shape.top(), shape.right(), shape.bottom());
                sizeY = 0;
            } else {
                sizeX = shape.width();
                sizeY = shape.height();
            }

            float innerGapX = sizeX - 2.f * strokeRadius;
            float innerGapY = sizeY - 2.f * strokeRadius;
            if ((innerGapX <= 0.f || innerGapY <= 0.f) && strokeRadius > 0.f) {
                // AA inset intersections are measured from the *outset* and remain marked as "solid"
                strokeInset = -strokeRadius;
            } else {
                // This will be upgraded to kFilledStrokeInterior if insets intersect
                centerWeight = kStrokeInterior;
                strokeInset = strokeRadius;
            }

            if (strokeRadius > 0.f || isLine) {
                // Regular strokes only need to upload 4 corner radii; hairline lines can be uploaded in
                // the same manner since it has no real corner radii.
                float joinStyle = draw.mJoinLimit;
                float lineFlag = isLine ? 1.f : 0.f;

                if (isLine || (sizeX == 0.f && sizeY == 0.f)) {
                    switch (draw.mStrokeCap) {
                        case Paint.CAP_ROUND:  joinStyle = -1.f; break; // round cap == round join
                        case Paint.CAP_BUTT:   joinStyle =  0.f; break; // butt cap == bevel join
                        case Paint.CAP_SQUARE: joinStyle =  1.f; break; // square cap == miter join
                    }
                } else if (draw.isMiterJoin()) {
                    // Normal corners are 90-degrees so become beveled if the miter limit is < sqrt(2).
                    // If the [r]rect has a width or height of 0, the corners are actually 180-degrees,
                    // so the must always be beveled (or, equivalently, butt-capped).
                    if (draw.mJoinLimit < MathUtil.SQRT2 || sizeX == 0.f || sizeY == 0.f) {
                        joinStyle = 0.f; // == bevel (or butt if width or height are zero)
                    } else {
                        // Discard actual miter limit because a 90-degree corner never exceeds it.
                        joinStyle = 1.f;
                    }
                } // else no join style correction needed for non-empty geometry or round joins

                memPutFloat(instanceData+16, -2.f);
                memPutFloat(instanceData+20, lineFlag);
                memPutFloat(instanceData+24, strokeRadius);
                memPutFloat(instanceData+28, joinStyle);
                memPutFloat(instanceData+32, isLine ? 0.f : shape.getRadius(RRect.kUpperLeftX));
                memPutFloat(instanceData+36, shape.getRadius(RRect.kUpperRightX));
                memPutFloat(instanceData+40, shape.getRadius(RRect.kLowerRightX));
                memPutFloat(instanceData+44, shape.getRadius(RRect.kLowerLeftX));
                memPutFloat(instanceData+48, shape.left());
                memPutFloat(instanceData+52, shape.top());
                memPutFloat(instanceData+56, shape.right());
                memPutFloat(instanceData+60, shape.bottom());
            } else {
                // Write -2 - cornerRadii to encode the X radii in such a way to trigger stroking but
                // guarantee the 2nd field is non-zero to signal hairline. Then we upload Y radii as
                // well to allow for elliptical hairlines.
                var radii = shape.getRadii();
                memPutFloat(instanceData+16, -2.f - radii[RRect.kUpperLeftX]);
                memPutFloat(instanceData+20, -2.f - radii[RRect.kUpperRightX]);
                memPutFloat(instanceData+24, -2.f - radii[RRect.kLowerRightX]);
                memPutFloat(instanceData+28, -2.f - radii[RRect.kLowerLeftX]);
                memPutFloat(instanceData+32, radii[RRect.kUpperLeftY]);
                memPutFloat(instanceData+36, radii[RRect.kUpperRightY]);
                memPutFloat(instanceData+40, radii[RRect.kLowerRightY]);
                memPutFloat(instanceData+44, radii[RRect.kLowerLeftY]);
                memPutFloat(instanceData+48, shape.left());
                memPutFloat(instanceData+52, shape.top());
                memPutFloat(instanceData+56, shape.right());
                memPutFloat(instanceData+60, shape.bottom());
            }
        } else {
            if (shape.isRect()) {
                // Rectangles (or rectangles embedded in an SkRRect) are converted to the
                // quadrilateral case, but with all edges anti-aliased (== -1).
                memPutFloat(instanceData + 16, -1.f);
                memPutFloat(instanceData + 20, -1.f);
                memPutFloat(instanceData + 24, -1.f);
                memPutFloat(instanceData + 28, -1.f);
                // xs
                memPutFloat(instanceData + 32, shape.left());
                memPutFloat(instanceData + 36, shape.right());
                memPutFloat(instanceData + 40, shape.right());
                memPutFloat(instanceData + 44, shape.left());
                // ys
                memPutFloat(instanceData + 48, shape.top());
                memPutFloat(instanceData + 52, shape.top());
                memPutFloat(instanceData + 56, shape.bottom());
                memPutFloat(instanceData + 60, shape.bottom());
            } else {
                // A filled rounded rectangle, so make sure at least one corner radii > 0 or the
                // shader won't detect it as a rounded rect.
                var radii = shape.getRadii();
                memPutFloat(instanceData + 16, radii[RRect.kUpperLeftX]);
                memPutFloat(instanceData + 20, radii[RRect.kUpperRightX]);
                memPutFloat(instanceData + 24, radii[RRect.kLowerRightX]);
                memPutFloat(instanceData + 28, radii[RRect.kLowerLeftX]);
                memPutFloat(instanceData + 32, radii[RRect.kUpperLeftY]);
                memPutFloat(instanceData + 36, radii[RRect.kUpperRightY]);
                memPutFloat(instanceData + 40, radii[RRect.kLowerRightY]);
                memPutFloat(instanceData + 44, radii[RRect.kLowerLeftY]);
                memPutFloat(instanceData + 48, shape.left());
                memPutFloat(instanceData + 52, shape.top());
                memPutFloat(instanceData + 56, shape.right());
                memPutFloat(instanceData + 60, shape.bottom());
            }
        }

        memPutFloat(instanceData+64, shape.centerX());
        memPutFloat(instanceData+68, shape.centerY());
        memPutFloat(instanceData+72, centerWeight);
        memPutFloat(instanceData+76, aaRadius);
        memPutFloat(instanceData+80, draw.getDepthAsFloat());
        draw.mTransform.store(instanceData+84);
        writer.endAppender();
    }
}
