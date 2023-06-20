/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.GeometryProcessor;
import icyllis.arc3d.engine.ManagedResource;
import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.arc3d.opengl.GLCore.glDeleteProgram;

/**
 * This class manages a GPU program and records per-program information. It also records the vertex
 * and instance attribute layouts that are to be used with the program.
 * <p>
 * Supports OpenGL 3.3 and OpenGL 4.5.
 */
public final class GLPipeline extends ManagedResource {

    private int mProgram;
    @SharedPtr
    private GLVertexArray mVertexArray;

    public GLPipeline(@Nonnull GLDevice device,
                       int program,
                       GLVertexArray vertexArray) {
        super(device);
        mProgram = program;
        mVertexArray = vertexArray;
    }

    @Nullable
    @SharedPtr
    public static GLPipeline make(@Nonnull GLDevice device,
                                  @Nonnull GeometryProcessor geomProc,
                                  int program) {
        @SharedPtr
        GLVertexArray vertexArray = GLVertexArray.make(device, geomProc);
        if (vertexArray == null) {
            return null;
        }
        return new GLPipeline(device,
                program,
                vertexArray);
    }

    @Override
    protected void deallocate() {
        if (mProgram != 0) {
            glDeleteProgram(mProgram);
        }
        discard();
    }

    public void discard() {
        mProgram = 0;
        mVertexArray = RefCnt.move(mVertexArray);
    }

    public int getProgram() {
        return mProgram;
    }

    public int getVertexArray() {
        return mVertexArray.getHandle();
    }

    /**
     * Set element buffer (index buffer).
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the element buffer object, raw ptr
     */
    public void bindIndexBuffer(@Nonnull GLBuffer buffer) {
        if (mVertexArray != null) {
            mVertexArray.bindIndexBuffer(buffer);
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next vertex data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first vertex data to the head of the buffer, in bytes
     */
    public void bindVertexBuffer(@Nonnull GLBuffer buffer, long offset) {
        if (mVertexArray != null) {
            mVertexArray.bindVertexBuffer(buffer, offset);
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next instance data, in bytes, is determined in constructor.
     * <p>
     * In OpenGL 3.3, bind pipeline first.
     *
     * @param buffer the vertex buffer object, raw ptr
     * @param offset first instance data to the head of the buffer, in bytes
     */
    public void bindInstanceBuffer(@Nonnull GLBuffer buffer, long offset) {
        if (mVertexArray != null) {
            mVertexArray.bindInstanceBuffer(buffer, offset);
        }
    }
}
