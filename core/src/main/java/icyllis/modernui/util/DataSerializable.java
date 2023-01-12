/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import java.io.*;

/**
 * {@code DataSerializable} is a serialization method alternative to standard Java
 * serialization. Objects can be written to a {@link DataOutput} and restored from
 * a {@link DataInput}, avoiding the heavy overhead of {@link Externalizable}.
 *
 * @since 3.7
 */
public interface DataSerializable {

    /**
     * The subclass implements the method to write its contents
     * by calling the methods of DataOutput for its primitive values.
     *
     * @param out the stream to write the object to
     * @throws IOException if an I/O error occurs
     */
    void writeData(DataOutput out) throws IOException;

    /**
     * The subclass implements the method to restore its contents
     * by calling the methods of DataInput for primitive types.
     * The method must read the values in the same sequence
     * and with the same types as were written by write method.
     *
     * @param in the stream to read data from in order to restore the object
     * @throws IOException if an I/O error occurs
     */
    void readData(DataInput in) throws IOException;
}
