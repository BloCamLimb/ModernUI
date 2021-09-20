/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.vertex;

import icyllis.modernui.graphics.GLBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents a set of vertex attribute specifications for a vertex shader.
 */
public class VertexFormat {

    private final Int2ObjectMap<List<VertexAttrib>> mAttributes;

    private int mVertexArray = INVALID_ID;

    /**
     * Creates a vertex format.
     * <p>
     * Attribute index depends on the order of <code>attribs</code> and bindings,
     * you must use explicit attribute layout for the base attribute index,
     * (layout = index). Each attribute takes up <code>getRepeat()</code> locations
     * in total. Binding points are given priority to assign.
     *
     * @param attribs all attributes
     */
    public VertexFormat(@Nonnull VertexAttrib... attribs) {
        if (attribs.length == 0) {
            throw new IllegalArgumentException("No attributes");
        }
        mAttributes = new Int2ObjectArrayMap<>(Arrays.stream(attribs)
                .sorted(Comparator.comparingInt(VertexAttrib::getBinding))
                .collect(Collectors.groupingBy(VertexAttrib::getBinding)));
    }

    /**
     * @return the shared vertex array object.
     */
    public int getVertexArray() {
        if (mVertexArray == INVALID_ID) {
            final int array = glCreateVertexArrays();
            int location = 0;
            for (var attribs : mAttributes.values()) {
                // relative offset in binding point
                int offset = 0;
                for (var a : attribs) {
                    offset = a.setFormat(array, location, offset);
                    location += a.getRepeat();
                }
            }
            mVertexArray = array;
            return array;
        }
        return mVertexArray;
    }

    /**
     * @return a set of used binding points for this VertexFormat
     */
    public IntSet getBindings() {
        return mAttributes.keySet();
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next vertex/instance data, in bytes, will
     * be {@link #getBindingSize(int)}
     *
     * @param binding the binding index
     * @param buffer  the vertex buffer
     * @param offset  first vertex/instance data to the head of the buffer, in bytes
     */
    public void setVertexBuffer(int binding, @Nonnull GLBuffer buffer, int offset) {
        glVertexArrayVertexBuffer(getVertexArray(), binding, buffer.get(), offset, getBindingSize(binding));
        /*ModernUI.LOGGER.info("Bind Vertex Buffer: {VAO={}, bind={}, buffer={}, offset={}, stride={}}",
                getVertexArray(), binding, buffer, offset, getBindingSize(binding));*/
    }

    /**
     * Set instanced array.
     *
     * @param binding the binding index
     * @param divisor instance divisor
     */
    public void setBindingDivisor(int binding, int divisor) {
        glVertexArrayBindingDivisor(getVertexArray(), binding, divisor);
    }

    /**
     * Set element buffer (index buffer).
     *
     * @param buffer the element buffer object
     */
    public void setElementBuffer(@Nonnull GLBuffer buffer) {
        glVertexArrayElementBuffer(getVertexArray(), buffer.get());
    }

    /**
     * Get the sum of the sizes of all attributes to the given binding point.
     *
     * @param binding the binding index
     * @return total size in bytes
     */
    public int getBindingSize(int binding) {
        List<VertexAttrib> attribs = mAttributes.get(binding);
        if (attribs != null) {
            int size = 0;
            for (var a : attribs) {
                size += a.getTotalSize();
            }
            return size;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VertexFormat that = (VertexFormat) o;

        return mAttributes.equals(that.mAttributes);
    }

    @Override
    public int hashCode() {
        return mAttributes.hashCode();
    }
}
