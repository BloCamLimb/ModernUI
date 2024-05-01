/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import java.util.ArrayList;

/**
 * A draw pass is subpass of a render pass.
 * <p>
 * Created immutable.
 */
public class DrawPass {

    @SharedPtr
    private ArrayList<GraphicsPipeline> mGraphicsPipelines = new ArrayList<>();

    private final DrawCommandList mCommandList = new DrawCommandList();

    private ImageProxy[] mSampledImages;

    public static DrawPass make(RecordingContext rContext,
                                DrawOpList drawOpList,
                                ImageProxy colorTarget,
                                byte loadStoreOps,
                                float[] clearColor) {
        var commandList = new DrawCommandList();




        return null;
    }

    @RawPtr
    public GraphicsPipeline getPipeline(int index) {
        return mGraphicsPipelines.get(index);
    }

    public DrawCommandList getCommandList() {
        return mCommandList;
    }

    public ImageProxy[] getSampledImages() {
        return mSampledImages;
    }
}
