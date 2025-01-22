/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.Vertices;
import icyllis.arc3d.engine.Caps;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.granite.geom.*;

/**
 * Granite defines a limited set of renderers in order to increase the likelihood of batching
 * across draw calls, and reducing the number of shader permutations required. These Renderers are
 * stateless singletons and remain alive for the life of the Context and its Recorders.
 * <p>
 * Because Renderers are immutable and the defined Renderers are created at context initialization,
 * RendererProvider is trivially thread-safe.
 */
public class RendererProvider {

    private static GeometryRenderer makeSingleStep(
            GeometryStep singleStep
    ) {
        String name = "SingleStep[" + singleStep.name() + "]";
        return new GeometryRenderer(name, singleStep);
    }

    // blur variant
    private final GeometryRenderer[] mSimpleBox = new GeometryRenderer[2];
    // mask format variant
    private final GeometryRenderer[] mRasterText = new GeometryRenderer[Engine.MASK_FORMAT_COUNT];
    // arc type variant
    private final GeometryRenderer[] mArc = new GeometryRenderer[ArcShape.kTypeCount];
    // has color, has tex, vertex mode variant
    private final GeometryRenderer[] mVertices = new GeometryRenderer[Vertices.kVertexModeCount*4];
    private final GeometryRenderer mPerEdgeAAQuad;
    private final GeometryRenderer mNonAABoundsFill;

    public RendererProvider(Caps caps, StaticBufferManager staticBufferManager) {
        mSimpleBox[0] = makeSingleStep(
                new AnalyticSimpleBoxStep(false)
        );
        mSimpleBox[1] = makeSingleStep(
                new AnalyticSimpleBoxStep(true)
        );
        for (int i = 0; i < Engine.MASK_FORMAT_COUNT; i++) {
            if (i == Engine.MASK_FORMAT_A565) continue;
            mRasterText[i] = makeSingleStep(
                    new RasterTextStep(i)
            );
        }
        for (int i = 0; i < ArcShape.kTypeCount; i++) {
            mArc[i] = makeSingleStep(
                    new AnalyticArcStep(i)
            );
        }
        for (int mode = 0; mode < Vertices.kVertexModeCount; mode++) {
            for (int mask = 0; mask < 4; mask++) {
                int index = mode * 4 + mask;
                mVertices[index] = makeSingleStep(
                        new VerticesStep(switch (mode) {
                            case Vertices.kPoints_VertexMode -> Engine.PrimitiveType.kPointList;
                            case Vertices.kLines_VertexMode -> Engine.PrimitiveType.kLineList;
                            case Vertices.kLineStrip_VertexMode -> Engine.PrimitiveType.kLineStrip;
                            case Vertices.kTriangles_VertexMode -> Engine.PrimitiveType.kTriangleList;
                            case Vertices.kTriangleStrip_VertexMode -> Engine.PrimitiveType.kTriangleStrip;
                            default -> throw new AssertionError();
                        }, (mask & 2) != 0, (mask & 1) != 0)
                );
            }
        }
        mPerEdgeAAQuad = makeSingleStep(
                new PerEdgeAAQuadStep(staticBufferManager)
        );
        mNonAABoundsFill = makeSingleStep(
                new CoverBoundsStep("non-aa-fill", CommonDepthStencilSettings.kDirectDepthGreaterPass)
        );
    }

    public GeometryRenderer getSimpleBox(boolean blur) {
        return mSimpleBox[blur ? 1 : 0];
    }

    public GeometryRenderer getRasterText(int maskFormat) {
        return mRasterText[maskFormat];
    }

    public GeometryRenderer getArc(int type) {
        return mArc[type];
    }

    public GeometryRenderer getVertices(int vertexMode, boolean hasColor, boolean hasTexCoords) {
        return mVertices[vertexMode*4 + (hasColor?2:0) + (hasTexCoords?1:0)];
    }

    public GeometryRenderer getPerEdgeAAQuad() {
        return mPerEdgeAAQuad;
    }

    // Non-AA bounds filling (can handle inverse "fills" but will touch every pixel within the clip)
    public GeometryRenderer getNonAABoundsFill() {
        return mNonAABoundsFill;
    }
}
