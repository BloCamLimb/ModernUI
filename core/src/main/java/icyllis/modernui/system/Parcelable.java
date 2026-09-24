/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@code Parcelable} is a serialization method alternative to standard Java
 * serialization. Instances can be written to and restored from a {@link Parcel},
 * avoiding the heavy overhead of {@link java.io.Externalizable}.
 * <br>
 * Classes implementing the {@code Parcelable} interface should be public, export
 * its package at least to {@code java.base}. Apps should also open their packages
 * that contain Parcelable classes to {@code icyllis.modernui.core} module to ensure
 * performance. Additionally, Parcelable classes must also have one of the
 * following two (ordered by priority) for construction:
 * <ol>
 *     <li>public constructor taking ({@link Parcel} src, {@link ClassLoader} loader)</li>
 *     <li>public constructor taking ({@link Parcel} src)</li>
 * </ol>
 * You may refer to {@link Creator} and {@link ClassLoaderCreator} to see the detailed
 * explanation of the parameters.
 *
 * <p>A typical implementation of {@code Parcelable} is:</p>
 *
 * <pre>{@code
 * public class MyParcelable implements Parcelable {
 *     private final int mData;
 *
 *     public MyParcelable(@NonNull Parcel src) {
 *         mData = src.readInt();
 *     }
 *
 *     @Override
 *     public void writeToParcel(@NonNull Parcel dest, int flags) {
 *         dest.writeInt(mData);
 *     }
 * }}</pre>
 *
 * @see Parcel
 * @since 3.7
 */
@ApiStatus.Experimental
public interface Parcelable {

    @ApiStatus.Internal
    @MagicConstant(flags = {
            PARCELABLE_WRITE_RETURN_VALUE
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface WriteFlags {
    }

    /**
     * Flag for use with {@link #writeToParcel}: the object being written
     * is a return value, that is the result of a function such as
     * <code>Parcelable someFunction()</code>,
     * <code>void someFunction(out Parcelable)</code>, or
     * <code>void someFunction(inout Parcelable)</code>.
     * <p>
     * This is used to transfer the ownership of the object, some implementations
     * may want to release resources at this point.
     */
    int PARCELABLE_WRITE_RETURN_VALUE = 0x0001;

    /**
     * The subclass implements the method to flatten its contents by calling
     * the methods of {@link Parcel} for its primitive values.
     *
     * @param dest  the parcel to write the object's data to
     * @param flags the flags about how the object should be written
     */
    void writeToParcel(@NonNull Parcel dest, @WriteFlags int flags);

    /**
     * The subclass implements the method to unflatten its contents by calling
     * the methods of {@link Parcel} for its primitive values.
     * <p>
     * This is an optional operation: if a subclass is designed to be immutable,
     * this method should not be implemented. The default implementation will
     * throw {@link UnsupportedOperationException}.
     *
     * @param src the parcel to read the object's data from
     */
    default void readFromParcel(@NonNull Parcel src) {
        throw new UnsupportedOperationException();
    }

    /**
     * Interface used to create {@link Parcelable} instances.
     * <p>
     * Subclasses should not implement this interface directly; instead, they should
     * declare a public constructor that matches the parameters of this interface.
     * Apps should also open their packages that contain Parcelable classes to
     * {@code icyllis.modernui.core} module to ensure performance.
     */
    @FunctionalInterface
    interface Creator<T extends Parcelable> {

        /**
         * Create a new instance of the {@link Parcelable} class, instantiating it
         * from the given {@link Parcel} whose data had previously been written by
         * {@link Parcelable#writeToParcel(Parcel, int)}.
         *
         * @param source the stream to read the object's data from
         * @return a new instance of the {@link Parcelable} class
         */
        T createFromParcel(@NonNull Parcel source);
    }

    /**
     * Specialization of {@link Creator} that allows you to receive the
     * {@link ClassLoader} the object is being created in.
     */
    @FunctionalInterface
    interface ClassLoaderCreator<T extends Parcelable> extends Creator<T> {

        @Override
        default T createFromParcel(@NonNull Parcel source) {
            return createFromParcel(source, null);
        }

        /**
         * Create a new instance of the {@link Parcelable} class, instantiating it
         * from the given {@link Parcel} whose data had previously been written by
         * {@link Parcelable#writeToParcel(Parcel, int)} and using the given {@link ClassLoader}.
         *
         * @param source the stream to read the object's data from
         * @param loader the class loader that this object is being created in
         * @return a new instance of the {@link Parcelable} class
         */
        T createFromParcel(@NonNull Parcel source, @Nullable ClassLoader loader);
    }
}
