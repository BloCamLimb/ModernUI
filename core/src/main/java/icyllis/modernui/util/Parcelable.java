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
 * Classes implementing the {@code Parcelable} interface must also have a
 * non-null static field called <var>CREATOR</var> of a type that implements the
 * {@link Creator} or {@link ClassLoaderCreator} interface.
 *
 * <p>A typical implementation of {@code Parcelable} is:</p>
 *
 * <pre>{@code
 * public class MyParcelable implements Parcelable {
 *
 *     public static final Parcelable.Creator<MyParcelable> CREATOR
 *             = MyParcelable::new;
 *
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
public interface Parcelable {

    @ApiStatus.Internal
    @MagicConstant()
    @Retention(RetentionPolicy.SOURCE)
    @interface WriteFlags {
    }

    /**
     * The subclass implements the method to flatten its contents by calling
     * the methods of {@link Parcel} for its primitive values.
     *
     * @param dest  the parcel to write the object's data to
     * @param flags the flags about how the object should be written
     */
    void writeToParcel(@NonNull Parcel dest, @WriteFlags int flags);

    /**
     * Interface that must be implemented and provided as a public <var>CREATOR</var>
     * field that creates instances of your {@link Parcelable} class from a {@link Parcel}.
     */
    @FunctionalInterface
    interface Creator<T> {

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
    interface ClassLoaderCreator<T> extends Creator<T> {

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
