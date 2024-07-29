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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.Paint;
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

    // AA variant
    private final GeometryRenderer[] mSimpleBox = new GeometryRenderer[2];
    // mask format variant
    private final GeometryRenderer[] mRasterText = new GeometryRenderer[Engine.MASK_FORMAT_COUNT];
    // cap variant
    private final GeometryRenderer[] mArc = new GeometryRenderer[Paint.CAP_COUNT];

    public RendererProvider(Caps caps) {
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
        for (int i = 0; i < Paint.CAP_COUNT; i++) {
            mArc[i] = makeSingleStep(
                    new AnalyticArcStep(i)
            );
        }
    }

    public GeometryRenderer getSimpleBox(boolean aa) {
        return mSimpleBox[aa ? 1 : 0];
    }

    public GeometryRenderer getRasterText(int maskFormat) {
        return mRasterText[maskFormat];
    }

    public GeometryRenderer getArc(int cap) {
        return mArc[cap];
    }
}
