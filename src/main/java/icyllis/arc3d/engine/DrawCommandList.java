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

package icyllis.arc3d.engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * The list that holds commands of a render pass.
 * The command types and arguments are flattened into an int array.
 */
public class DrawCommandList extends IntArrayList {

    /**
     * <pre><code>
     * struct BindGraphicsPipeline {
     *     int pipelineIndex;
     * }</code></pre>
     */
    public static final int CMD_BIND_GRAPHICS_PIPELINE = 0;
    public static final int CMD_DRAW = 1;
    public static final int CMD_DRAW_INDEXED = 2;
    public static final int CMD_DRAW_INSTANCED = 3;
    public static final int CMD_DRAW_INDEXED_INSTANCED = 4;

    public void bindGraphicsPipeline(int pipelineIndex) {
        add(CMD_BIND_GRAPHICS_PIPELINE);
        add(pipelineIndex);
    }

    public void draw(int vertexCount, int baseVertex) {
        add(CMD_DRAW);
        add(vertexCount);
        add(baseVertex);
    }

    public final void drawIndexed(int indexCount, int baseIndex,
                                  int baseVertex) {
        add(CMD_DRAW_INDEXED);
        add(indexCount);
        add(baseIndex);
        add(baseVertex);
    }

    public final void drawInstanced(int instanceCount, int baseInstance,
                                    int vertexCount, int baseVertex) {
        add(CMD_DRAW_INSTANCED);
        add(instanceCount);
        add(baseInstance);
        add(vertexCount);
        add(baseVertex);
    }

    public final void drawIndexedInstanced(int indexCount, int baseIndex,
                                           int instanceCount, int baseInstance,
                                           int baseVertex) {
        add(CMD_DRAW_INDEXED_INSTANCED);
        add(indexCount);
        add(baseIndex);
        add(instanceCount);
        add(baseInstance);
        add(baseVertex);
    }
}
