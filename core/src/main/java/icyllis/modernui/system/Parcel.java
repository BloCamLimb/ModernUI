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
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.Log;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * A Parcel is a message container for a sequence of bytes, that performs
 * non-blocking binary I/O on various data objects.
 * <p>
 * Parcel provides methods for converting arbitrary objects to and from binaries.
 * This is mainly used for in-memory communication between activities, network
 * communication between clients, and inter-process communication. It may not be
 * ideal for persistent storage.
 *
 * @see Parcelable
 * @since 3.9
 */
//TODO redesign in future
@ApiStatus.Experimental
public final class Parcel {

    private static final Marker MARKER = MarkerFactory.getMarker("Parcel");

    /**
     * Value types, version 3.7, do not change.
     */
    private static final byte VAL_NULL = 0;
    private static final byte
            VAL_BYTE = 1,
            VAL_SHORT = 2,
            VAL_INT = 3,
            VAL_LONG = 4,
            VAL_FLOAT = 5,
            VAL_DOUBLE = 6,
            VAL_BOOLEAN = 7,
            VAL_CHAR = 8;
    private static final byte
            VAL_BYTE_ARRAY = 9,
            VAL_SHORT_ARRAY = 10,
            VAL_INT_ARRAY = 11,
            VAL_LONG_ARRAY = 12,
            VAL_FLOAT_ARRAY = 13,
            VAL_DOUBLE_ARRAY = 14,
            VAL_BOOLEAN_ARRAY = 15,
            VAL_CHAR_ARRAY = 16;
    private static final byte
            VAL_STRING = 17,
            VAL_UUID = 19,
            VAL_INSTANT = 20;
    private static final byte
            VAL_DATA_SET = 64,
            VAL_PARCELABLE = 65,
            VAL_CHAR_SEQUENCE = 66,
            VAL_LIST = 68,
            VAL_OBJECT_ARRAY = 118,
            VAL_SERIALIZABLE = 127;


    private ByteBuffer mNativeBuffer;

    /**
     * @see #freeData()
     */
    @ApiStatus.Internal
    public Parcel() {
    }

    private void ensureCapacity(int len) {
        if (mNativeBuffer != null && mNativeBuffer.remaining() >= len) {
            return;
        }
        long size = (mNativeBuffer == null ? 0 : mNativeBuffer.limit()) + len;
        size += size >> 1;
        if (size > Integer.MAX_VALUE) {
            throw new BufferOverflowException();
        }
        setCapacity((int) Math.max(size, 128));
    }

    private void setCapacity(int size) {
        if (mNativeBuffer != null && mNativeBuffer.capacity() >= size) {
            mNativeBuffer.limit(size);
        } else if (mNativeBuffer == null) {
            mNativeBuffer = MemoryUtil.memAlloc(size);
        } else {
            mNativeBuffer = MemoryUtil.memRealloc(mNativeBuffer, size);
        }
    }

    public int position() {
        return mNativeBuffer == null ? 0 : mNativeBuffer.position();
    }

    public void position(int newPosition) {
        ensureCapacity(0);
        mNativeBuffer.position(newPosition);
    }

    public int limit() {
        return mNativeBuffer == null ? 0 : mNativeBuffer.limit();
    }

    public void limit(int newLimit) {
        ensureCapacity(0);
        mNativeBuffer.limit(newLimit);
    }

    public int capacity() {
        return mNativeBuffer == null ? 0 : mNativeBuffer.capacity();
    }

    public void writeBytes(byte[] src) {
        writeBytes(src, 0, src.length);
    }

    public void writeBytes(byte[] src, int off, int len) {
        ensureCapacity(len);
        mNativeBuffer.put(src, off, len);
    }

    /**
     * Write a boolean value into the parcel.
     */
    public void writeBoolean(boolean b) {
        writeByte(b ? 1 : 0);
    }

    /**
     * Write a char value into the parcel.
     */
    public void writeChar(int v) {
        writeShort(v);
    }

    /**
     * Write a byte value into the parcel.
     */
    public void writeByte(int v) {
        ensureCapacity(1);
        mNativeBuffer.put((byte) v);
    }

    /**
     * Write a short integer value into the parcel.
     */
    public void writeShort(int v) {
        ensureCapacity(2);
        mNativeBuffer.putShort((short) v);
    }

    /**
     * Write an integer value into the parcel.
     */
    public void writeInt(int v) {
        ensureCapacity(4);
        mNativeBuffer.putInt(v);
    }

    /**
     * Write a long integer value into the parcel.
     */
    public void writeLong(long v) {
        ensureCapacity(8);
        mNativeBuffer.putLong(v);
    }

    /**
     * Write a floating point value into the parcel.
     */
    public void writeFloat(float v) {
        writeInt(Float.floatToRawIntBits(v));
    }

    /**
     * Write a double precision floating point value into the parcel.
     */
    public void writeDouble(double v) {
        writeLong(Double.doubleToRawLongBits(v));
    }

    public void readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
    }

    public void readBytes(byte[] dst, int off, int len) {
        if (mNativeBuffer == null) {
            throw new BufferUnderflowException();
        }
        mNativeBuffer.get(dst, off, len);
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public char readChar() {
        return (char) readShort();
    }

    public byte readByte() {
        if (mNativeBuffer == null) {
            throw new BufferUnderflowException();
        }
        return mNativeBuffer.get();
    }

    public short readShort() {
        if (mNativeBuffer == null) {
            throw new BufferUnderflowException();
        }
        return mNativeBuffer.getShort();
    }

    public int readInt() {
        if (mNativeBuffer == null) {
            throw new BufferUnderflowException();
        }
        return mNativeBuffer.getInt();
    }

    public long readLong() {
        if (mNativeBuffer == null) {
            throw new BufferUnderflowException();
        }
        return mNativeBuffer.getLong();
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Write a value and its type.
     *
     * @param v the value to write
     */
    public void writeValue(@Nullable Object v) {
        if (v == null) {
            writeByte(VAL_NULL);
        } else if (v instanceof String) {
            writeByte(VAL_STRING);
            writeString((String) v);
        } else if (v instanceof Integer) {
            writeByte(VAL_INT);
            writeInt((Integer) v);
        } else if (v instanceof Long) {
            writeByte(VAL_LONG);
            writeLong((Long) v);
        } else if (v instanceof Float) {
            writeByte(VAL_FLOAT);
            writeFloat((Float) v);
        } else if (v instanceof Double) {
            writeByte(VAL_DOUBLE);
            writeDouble((Double) v);
        } else if (v instanceof Byte) {
            writeByte(VAL_BYTE);
            writeByte((Byte) v);
        } else if (v instanceof Short) {
            writeByte(VAL_SHORT);
            writeShort((Short) v);
        } else if (v instanceof Character) {
            writeByte(VAL_CHAR);
            writeChar((Character) v);
        } else if (v instanceof Boolean) {
            writeByte(VAL_BOOLEAN);
            writeBoolean((Boolean) v);
        } else if (v instanceof UUID) {
            writeByte(VAL_UUID);
            writeUUID((UUID) v);
        } else if (v instanceof int[]) {
            writeByte(VAL_INT_ARRAY);
            writeIntArray((int[]) v);
        } else if (v instanceof byte[]) {
            writeByte(VAL_BYTE_ARRAY);
            writeByteArray((byte[]) v);
        } else if (v instanceof char[]) {
            writeByte(VAL_CHAR_ARRAY);
            writeCharArray((char[]) v);
        } else if (v instanceof DataSet) {
            writeByte(VAL_DATA_SET);
            writeDataSet((DataSet) v);
        } else if (v instanceof Parcelable) {
            writeByte(VAL_PARCELABLE);
            writeParcelable((Parcelable) v, 0);
        } else if (v instanceof CharSequence) {
            writeByte(VAL_CHAR_SEQUENCE);
            writeCharSequence((CharSequence) v);
        } else if (v instanceof List) {
            writeByte(VAL_LIST);
            writeList((List<?>) v);
        } else if (v instanceof long[]) {
            writeByte(VAL_LONG_ARRAY);
            writeLongArray((long[]) v);
        } else if (v instanceof short[]) {
            writeByte(VAL_SHORT_ARRAY);
            writeShortArray((short[]) v);
        } else if (v instanceof float[]) {
            writeByte(VAL_FLOAT_ARRAY);
            writeFloatArray((float[]) v);
        } else if (v instanceof double[]) {
            writeByte(VAL_DOUBLE_ARRAY);
            writeDoubleArray((double[]) v);
        } else if (v instanceof boolean[]) {
            writeByte(VAL_BOOLEAN_ARRAY);
            writeBooleanArray((boolean[]) v);
        } else {
            Class<?> clazz = v.getClass();
            if (clazz.isArray() && clazz.getComponentType() == Object.class) {
                // pure Object[]
                writeByte(VAL_OBJECT_ARRAY);
                writeArray((Object[]) v);
            } else if (v instanceof Serializable value) {
                writeByte(VAL_SERIALIZABLE);
                String name = value.getClass().getName();
                writeString(name);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(value);
                    oos.close();
                    writeByteArray(baos.toByteArray());
                } catch (IOException ioe) {
                    throw new BadParcelableException("Parcelable encountered "
                            + "IOException writing serializable object (name = "
                            + name + ")", ioe);
                }
            }
            // others are silently ignored
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T readValue(@Nullable ClassLoader loader,
                           @Nullable Class<T> clazz, @Nullable Class<?> elemType) {
        final byte type = readByte();
        final Object object = switch (type) {
            case VAL_NULL -> null;
            case VAL_BYTE -> readByte();
            case VAL_SHORT -> readShort();
            case VAL_INT -> readInt();
            case VAL_LONG -> readLong();
            case VAL_FLOAT -> readFloat();
            case VAL_DOUBLE -> readDouble();
            case VAL_BOOLEAN -> readBoolean();
            case VAL_CHAR -> readChar();
            case VAL_BYTE_ARRAY -> readByteArray();
            case VAL_SHORT_ARRAY -> readShortArray();
            case VAL_INT_ARRAY -> readIntArray();
            case VAL_LONG_ARRAY -> readLongArray();
            case VAL_FLOAT_ARRAY -> readFloatArray();
            case VAL_DOUBLE_ARRAY -> readDoubleArray();
            case VAL_BOOLEAN_ARRAY -> readBooleanArray();
            case VAL_CHAR_ARRAY -> readCharArray();
            case VAL_STRING -> readString();
            case VAL_UUID -> readUUID();
            case VAL_DATA_SET -> readDataSet(loader);
            case VAL_PARCELABLE -> readParcelable0(loader, (Class<? extends Parcelable>) clazz);
            case VAL_CHAR_SEQUENCE -> readCharSequence();
            case VAL_LIST -> readList(loader, elemType);
            case VAL_OBJECT_ARRAY -> {
                if (elemType == null) {
                    elemType = Object.class;
                }
                if (clazz != null) {
                    if (!clazz.isArray()) {
                        throw new BadParcelableException("About to read an array but type "
                                + clazz.getCanonicalName()
                                + " required by caller is not an array.");
                    }
                    Class<?> itemArrayType = elemType.arrayType();
                    if (!clazz.isAssignableFrom(itemArrayType)) {
                        throw new BadParcelableException("About to read a " + itemArrayType.getCanonicalName()
                                + ", which is not a subtype of type " + clazz.getCanonicalName()
                                + " required by caller.");
                    }
                }
                yield readArray(loader, elemType);
            }
            default -> throw new BadParcelableException("Unknown value type identifier: " + type);
        };
        if (object != null && clazz != null && !clazz.isInstance(object)) {
            throw new BadParcelableException("Deserialized object " + object
                    + " is not an instance of required class " + clazz.getName()
                    + " provided in the parameter");
        }
        return (T) object;
    }

    /**
     * Flatten the name of the class of the Parcelable and its contents
     * into the parcel.
     *
     * @param p               The Parcelable object to be written.
     * @param parcelableFlags Contextual flags as per
     *                        {@link Parcelable#writeToParcel(Parcel, int) Parcelable.writeToParcel()}.
     */
    public final void writeParcelable(@Nullable Parcelable p, @Parcelable.WriteFlags int parcelableFlags) {
        if (p == null) {
            writeString(null);
            return;
        }
        writeParcelableCreator(p);
        p.writeToParcel(this, parcelableFlags);
    }

    /**
     * Flatten the name of the class of the Parcelable into this Parcel.
     *
     * @param p The Parcelable object to be written.
     * @see #readParcelableCreator
     */
    public final void writeParcelableCreator(@NonNull Parcelable p) {
        String name = p.getClass().getName();
        writeString(name);
    }

    @Nullable
    public <T extends Parcelable> T readParcelable(@Nullable ClassLoader loader,
                                                   @NonNull Class<T> clazz) {
        return readParcelable0(loader, Objects.requireNonNull(clazz));
    }

    @Nullable
    public <T extends Parcelable> T readParcelable0(@Nullable ClassLoader loader,
                                                    @Nullable Class<T> clazz) {
        Parcelable.Creator<T> creator = readParcelableCreator0(loader, clazz);
        if (creator == null) {
            return null;
        }
        if (creator instanceof Parcelable.ClassLoaderCreator<T>) {
            return ((Parcelable.ClassLoaderCreator<T>) creator).createFromParcel(this, loader);
        }
        return creator.createFromParcel(this);
    }

    @Nullable
    public <T extends Parcelable> Parcelable.Creator<T> readParcelableCreator(
            @Nullable ClassLoader loader,
            @NonNull Class<T> clazz) {
        return readParcelableCreator0(loader, Objects.requireNonNull(clazz));
    }

    // ModernUI changed: fast lookup cache
    private static final class NameCache extends HashMap<String, WeakReference<Class<?>>> {
        private final StampedLock lock = new StampedLock();

        Class<?> compute(@NonNull ClassLoader loader, @NonNull String name) {
            long stamp = lock.tryOptimisticRead();
            WeakReference<Class<?>> value = get(name);
            if (lock.validate(stamp)) {
                if (value != null) return value.get();
            } else {
                stamp = lock.readLock();
                try {
                    value = get(name);
                    if (value != null) return value.get();
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            Class<?> target;
            try {
                target = loader.loadClass(name);
                if (!Parcelable.class.isAssignableFrom(target)) {
                    throw new BadParcelableException("Parcelable protocol requires subclassing "
                            + "from Parcelable on " + target);
                }
            } catch (ClassNotFoundException e) {
                throw new BadParcelableException(
                        "ClassNotFoundException when unmarshalling: " + name, e);
            }
            WeakReference<Class<?>> computed = new WeakReference<>(target);

            long ws = lock.writeLock();
            try {
                WeakReference<Class<?>> existing = putIfAbsent(name, computed);
                if (existing == null) {
                    return computed.get();
                } else {
                    // there's race, just discard the newly created value
                    return existing.get();
                }
            } finally {
                lock.unlockWrite(ws);
            }
        }
    }

    // it's safe to use WeakHashMap with StampedLock, expungeStaleEntries is safe
    private static final WeakHashMap<ClassLoader, NameCache> sClassCache = new WeakHashMap<>();
    private static final StampedLock sCacheLock = new StampedLock();

    // ModernUI changed: fast lookup cache
    private static NameCache getClassNameCache(@NonNull ClassLoader loader) {
        long stamp = sCacheLock.tryOptimisticRead();
        NameCache value = sClassCache.get(loader);
        if (sCacheLock.validate(stamp)) {
            if (value != null) return value;
        } else {
            stamp = sCacheLock.readLock();
            try {
                value = sClassCache.get(loader);
                if (value != null) return value;
            } finally {
                sCacheLock.unlockRead(stamp);
            }
        }

        final NameCache computed = new NameCache();

        long ws = sCacheLock.writeLock();
        try {
            NameCache existing = sClassCache.putIfAbsent(loader, computed);
            if (existing == null) {
                return computed;
            } else {
                // there's race, just discard the newly created value
                return existing;
            }
        } finally {
            sCacheLock.unlockWrite(ws);
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static Class<? extends Parcelable> getParcelableClass(@NonNull ClassLoader loader, @NonNull String name) {
        Objects.requireNonNull(loader);
        Objects.requireNonNull(name);
        NameCache classCache = getClassNameCache(loader);
        Class<?> target = classCache.compute(loader, name);
        Objects.requireNonNull(target);
        return (Class<? extends Parcelable>) target;
    }

    // ModernUI changed:
    private static Parcelable.Creator<?> makeFactory(@NonNull Class<? extends Parcelable> type) {
        var lookup = MethodHandles.lookup();
        try {
            // try private version first, each createFromParcel is about 2.5ns
            var privLookup = MethodHandles.privateLookupIn(type, lookup);
            // declare a hidden class in target class to avoid class loader leaks
            try {
                var ctor = privLookup.findConstructor(
                        type,
                        MethodType.methodType(void.class, Parcel.class, ClassLoader.class)
                );
                var cs = LambdaMetafactory.metafactory(
                        privLookup,
                        "createFromParcel",
                        MethodType.methodType(Parcelable.ClassLoaderCreator.class),
                        MethodType.methodType(Parcelable.class, Parcel.class, ClassLoader.class),
                        ctor,
                        MethodType.methodType(type, Parcel.class, ClassLoader.class)
                );
                return (Parcelable.ClassLoaderCreator<?>) cs.getTarget().invoke();
            } catch (NoSuchMethodException ignored) {
                // fallback
            } catch (Throwable e) {
                Log.LOGGER.error(MARKER, "Unexpected error during making Parcelable.ClassLoaderCreator", e);
            }
            try {
                var ctor = privLookup.findConstructor(
                        type,
                        MethodType.methodType(void.class, Parcel.class)
                );
                var cs = LambdaMetafactory.metafactory(
                        privLookup,
                        "createFromParcel",
                        MethodType.methodType(Parcelable.Creator.class),
                        MethodType.methodType(Parcelable.class, Parcel.class),
                        ctor,
                        MethodType.methodType(type, Parcel.class)
                );
                return (Parcelable.Creator<?>) cs.getTarget().invoke();
            } catch (NoSuchMethodException ignored) {
                // fallback
            } catch (Throwable e) {
                Log.LOGGER.error(MARKER, "Unexpected error during making Parcelable.Creator", e);
            }
        } catch (IllegalAccessException ignored) {
            // no private access, fallback
        }

        // In the public version, we will use an adapter instead of a hidden class,
        // because we don't know where to declare the hidden class.
        // Since LambdaMetafactory will use ClassOption.STRONG, if we declare in this class,
        // it will cause target class loader leak. Each createFromParcel is about 6ns
        try {
            var ctor = lookup.findConstructor(
                    type,
                    MethodType.methodType(void.class, Parcel.class, ClassLoader.class)
            ).asType(
                    MethodType.methodType(Parcelable.class, Parcel.class, ClassLoader.class)
            );
            // make an adapter
            return (Parcelable.ClassLoaderCreator<?>) (src, loader) -> {
                try {
                    return (Parcelable) ctor.invokeExact(src, loader);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException ignored) {
            // fallback
        } catch (IllegalAccessException e) {
            throw new BadParcelableException("IllegalAccessException when unmarshalling: ", e);
        }
        try {
            var ctor = lookup.findConstructor(
                    type,
                    MethodType.methodType(void.class, Parcel.class)
            ).asType(
                    MethodType.methodType(Parcelable.class, Parcel.class)
            );
            // make an adapter
            return src -> {
                try {
                    return (Parcelable) ctor.invokeExact(src);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException ignored) {
            // fallback
        } catch (IllegalAccessException e) {
            throw new BadParcelableException("IllegalAccessException when unmarshalling: ", e);
        }

        throw new BadParcelableException("Parcelable protocol requires a public "
                + "constructor taking either (Parcel, ClassLoader) or (Parcel) "
                + "on " + type);
    }

    // ModernUI changed: use ClassValue for better lookup performance and to avoid class loader leaks
    private static final ClassValue<Parcelable.Creator<?>>
            gCreators = new ClassValue<>() {
        @SuppressWarnings("unchecked")
        @Override
        protected Parcelable.Creator<?> computeValue(@NonNull Class<?> type) {
            return makeFactory((Class<? extends Parcelable>) type);
        }
    };

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T extends Parcelable> Parcelable.Creator<T> getParcelableCreator(@NonNull Class<T> type) {
        Objects.requireNonNull(type);
        Parcelable.Creator<?> creator = gCreators.get(type);
        Objects.requireNonNull(creator);
        return (Parcelable.Creator<T>) creator;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends Parcelable> Parcelable.Creator<T> readParcelableCreator0(
            @Nullable ClassLoader loader,
            @Nullable Class<T> clazz) {
        final var name = readString();
        if (name == null) {
            return null;
        }

        ClassLoader actualLoader = (loader == null ? Parcel.class.getClassLoader() : loader);
        NameCache classCache = getClassNameCache(actualLoader);

        Class<?> target = classCache.compute(actualLoader, name);
        if (clazz != null) {
            if (!clazz.isAssignableFrom(target)) {
                throw new BadParcelableException("Parcelable " + target + " is not "
                        + "a subclass of required " + clazz
                        + " provided in the parameter");
            }
        }
        Parcelable.Creator<?> creator = gCreators.get(target);
        Objects.requireNonNull(creator);

        return (Parcelable.Creator<T>) creator;
    }

    /**
     * Write a byte array.
     *
     * @param b the bytes to write
     */
    public void writeByteArray(@Nullable byte[] b) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        writeInt(b.length);
        writeBytes(b);
    }

    /**
     * Write a byte array.
     *
     * @param b the bytes to write
     */
    public void writeByteArray(@Nullable byte[] b, int off, int len) {
        if (b == null) {
            writeInt(-1);
            return;
        }
        writeInt(len);
        writeBytes(b, off, len);
    }

    @Nullable
    public byte[] readByteArray() {
        int n = readInt();
        if (n < 0)
            return null;
        byte[] b = new byte[n];
        readBytes(b, 0, n);
        return b;
    }

    /**
     * Write a short array.
     *
     * @param value the short array to write
     */
    public void writeShortArray(@Nullable short[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (short e : value)
            writeShort(e);
    }

    @Nullable
    public short[] readShortArray() {
        int n = readInt();
        if (n < 0)
            return null;
        short[] value = new short[n];
        for (int i = 0; i < n; i++)
            value[i] = readShort();
        return value;
    }

    /**
     * Write an int array.
     *
     * @param value the int array to write
     */
    public void writeIntArray(@Nullable int[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (int e : value)
            writeInt(e);
    }

    @Nullable
    public int[] readIntArray() {
        int n = readInt();
        if (n < 0)
            return null;
        int[] value = new int[n];
        for (int i = 0; i < n; i++)
            value[i] = readInt();
        return value;
    }

    /**
     * Write a long array.
     *
     * @param value the long array to write
     */
    public void writeLongArray(@Nullable long[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (long e : value)
            writeLong(e);
    }

    @Nullable
    public long[] readLongArray() {
        int n = readInt();
        if (n < 0)
            return null;
        long[] value = new long[n];
        for (int i = 0; i < n; i++)
            value[i] = readLong();
        return value;
    }

    /**
     * Write a float array.
     *
     * @param value the float array to write
     */
    public void writeFloatArray(@Nullable float[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (float e : value)
            writeFloat(e);
    }

    @Nullable
    public float[] readFloatArray() {
        int n = readInt();
        if (n < 0)
            return null;
        float[] value = new float[n];
        for (int i = 0; i < n; i++)
            value[i] = readFloat();
        return value;
    }

    /**
     * Write a double array.
     *
     * @param value the double array to write
     */
    public void writeDoubleArray(@Nullable double[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (double e : value)
            writeDouble(e);
    }

    @Nullable
    public double[] readDoubleArray() {
        int n = readInt();
        if (n < 0)
            return null;
        double[] value = new double[n];
        for (int i = 0; i < n; i++)
            value[i] = readDouble();
        return value;
    }

    /**
     * Write a boolean array.
     *
     * @param value the boolean array to write
     */
    public void writeBooleanArray(@Nullable boolean[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (boolean e : value)
            writeBoolean(e);
    }

    @Nullable
    public boolean[] readBooleanArray() {
        int n = readInt();
        if (n < 0)
            return null;
        boolean[] value = new boolean[n];
        for (int i = 0; i < n; i++)
            value[i] = readBoolean();
        return value;
    }

    /**
     * Write a char array.
     *
     * @param value the char array to write
     */
    public void writeCharArray(@Nullable char[] value) {
        if (value == null) {
            writeInt(-1);
            return;
        }
        writeInt(value.length);
        for (char e : value)
            writeChar(e);
    }

    @Nullable
    public char[] readCharArray() {
        int n = readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = readChar();
        return value;
    }

    /**
     * Write an object array.
     *
     * @param a the object array to write
     */
    public void writeArray(@Nullable Object[] a) {
        if (a == null) {
            writeInt(-1);
            return;
        }
        writeInt(a.length);
        for (var e : a) {
            writeValue(e);
        }
    }

    /**
     * Read an object array.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T[] readArray(@Nullable ClassLoader loader,
                             @NonNull Class<T> clazz) {
        int n = readInt();
        if (n < 0)
            return null;
        T[] a = (T[]) (clazz == Object.class ? new Object[n] : Array.newInstance(clazz, n));
        for (int i = 0; i < n; i++) {
            T value = readValue(loader, clazz, null);
            a[i] = value;
        }
        return a;
    }

    /**
     * Write a string.
     *
     * @param s the string to write
     */
    public void writeString(@Nullable String s) {
        writeString16(s);
    }

    /**
     * Write a string in UTF-8 format.
     *
     * @param s the string to write
     */
    public void writeString8(@Nullable String s) {
        if (s == null) {
            writeInt(-1);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            writeBytes(bytes);
        }
    }

    /**
     * Write a string in UTF-16 format.
     *
     * @param s the string to write
     */
    public void writeString16(@Nullable String s) {
        if (s == null) {
            writeInt(-1);
        } else {
            int len = s.length();
            writeInt(len);
            for (int i = 0; i < len; i++) {
                writeChar(s.charAt(i));
            }
        }
    }

    /**
     * Read a string.
     */
    @Nullable
    public String readString() {
        return readString16();
    }

    /**
     * Read a string in UTF-8 format.
     */
    @Nullable
    public String readString8() {
        int n = readInt();
        if (n < 0)
            return null;
        byte[] bytes = new byte[n];
        readBytes(bytes, 0, n);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a string in UTF-16 format.
     */
    @Nullable
    public String readString16() {
        int n = readInt();
        if (n < 0)
            return null;
        char[] value = new char[n];
        for (int i = 0; i < n; i++)
            value[i] = readChar();
        return new String(value);
    }

    /**
     * Write a CharSequence value into the parcel. May be Spanned.
     */
    public void writeCharSequence(@Nullable CharSequence cs) {
        TextUtils.writeToParcel(cs, this, 0);
    }

    /**
     * Read a CharSequence value from the parcel. May be Spanned.
     */
    @Nullable
    public CharSequence readCharSequence() {
        return TextUtils.createFromParcel(this);
    }

    /**
     * Write a list.
     *
     * @param list the list to write
     */
    public void writeList(@Nullable List<?> list) {
        if (list == null) {
            writeInt(-1);
            return;
        }
        writeInt(list.size());
        for (var e : list) {
            writeValue(e);
        }
    }

    /**
     * Read a list as a value.
     *
     * @return the newly created list
     */
    @Nullable
    private <T> List<T> readList(@Nullable ClassLoader loader,
                                 @Nullable Class<? extends T> clazz) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        var res = new ArrayList<T>(n);
        while (n-- != 0) {
            res.add(readValue(loader, clazz, null));
        }
        return res;
    }

    /**
     * Write a data set.
     *
     * @param source the data set to write
     */
    @Deprecated
    public void writeDataSet(@Nullable DataSet source) {
        if (source == null) {
            writeInt(-1);
            return;
        }
        writeInt(source.size());
        for (var e : source.entrySet()) {
            writeString(e.getKey());
            writeValue(e.getValue());
        }
    }

    /**
     * Read a data set as a value.
     *
     * @param loader the class loader for {@link Parcelable} classes
     * @return the newly created data set
     */
    @Deprecated
    @Nullable
    public DataSet readDataSet(@Nullable ClassLoader loader) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        var res = new DataSet(n);
        while (n-- != 0) {
            res.put(readString(), readValue(loader, null, null));
        }
        return res;
    }

    /**
     * Write UUID as a value.
     */
    public void writeUUID(@NonNull UUID value) {
        writeLong(value.getMostSignificantBits());
        writeLong(value.getLeastSignificantBits());
    }

    /**
     * Read UUID as a value.
     */
    @NonNull
    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    @ApiStatus.Internal
    public void freeData() {
        if (mNativeBuffer != null) {
            MemoryUtil.memFree(mNativeBuffer);
            mNativeBuffer = null;
        }
    }
}
