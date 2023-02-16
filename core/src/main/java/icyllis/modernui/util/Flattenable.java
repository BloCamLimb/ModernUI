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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;

/**
 * {@code Flattenable} is a serialization method alternative to standard Java
 * serialization. Instances can be written to a {@link DataOutput} and restored
 * from a {@link DataInput}, avoiding the heavy overhead of {@link Externalizable}.
 * <br>
 * Classes implementing the {@code Flattenable} interface must also have a
 * non-null static field called <var>CREATOR</var> of a type that implements the
 * {@link Creator} or {@link ClassLoaderCreator} interface.
 *
 * <p>A typical implementation of {@code Flattenable} is:</p>
 *
 * <pre>{@code
 * public class MyFlattenable implements Flattenable {
 *
 *     public static final Flattenable.Creator<MyFlattenable> CREATOR
 *             = MyFlattenable::new;
 *
 *     private int mData;
 *
 *     private MyFlattenable(DataInput in) throws IOException {
 *         mData = in.readInt();
 *     }
 *
 *     @Override
 *     public void write(DataOutput out) throws IOException {
 *         out.writeInt(mData);
 *     }
 * }}</pre>
 *
 * @since 3.7
 */
public interface Flattenable {

    /**
     * The subclass implements the method to flatten its contents by calling
     * the methods of {@link DataOutput} for its primitive values.
     *
     * @param dest the stream to write the object's data to
     * @throws IOException if an I/O error occurs
     */
    void write(@Nonnull DataOutput dest) throws IOException;

    /**
     * Interface that must be implemented and provided as a public <var>CREATOR</var>
     * field that creates instances of your {@link Flattenable} class from a {@link DataInput}.
     */
    @FunctionalInterface
    interface Creator<T> {

        /**
         * Create a new instance of the {@link Flattenable} class, instantiating it
         * from the given {@link DataInput} whose data had previously been written by
         * {@link Flattenable#write(DataOutput)}.
         *
         * @param source the stream to read the object's data from
         * @return a new instance of the {@link Flattenable} class
         * @throws IOException if an I/O error occurs
         */
        T create(@Nonnull DataInput source) throws IOException;
    }

    /**
     * Specialization of {@link Creator} that allows you to receive the
     * {@link ClassLoader} the object is being created in.
     */
    @FunctionalInterface
    interface ClassLoaderCreator<T> extends Creator<T> {

        @Override
        default T create(@Nonnull DataInput source) throws IOException {
            return create(source, null);
        }

        /**
         * Create a new instance of the {@link Flattenable} class, instantiating it
         * from the given {@link DataInput} whose data had previously been written by
         * {@link Flattenable#write(DataOutput)} and using the given {@link ClassLoader}.
         *
         * @param source the stream to read the object's data from
         * @param loader the class loader that this object is being created in
         * @return a new instance of the {@link Flattenable} class
         * @throws IOException if an I/O error occurs
         */
        T create(@Nonnull DataInput source, @Nullable ClassLoader loader) throws IOException;
    }
}
