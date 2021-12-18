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

package icyllis.modernui.test;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Fork(1)
@Threads(2)
@Warmup(iterations = 1, time = 3)
@Measurement(iterations = 3, time = 3)
public class TestBenchmark {

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(TestBenchmark.class.getSimpleName())
                .shouldFailOnError(true).shouldDoGC(true)
                .jvmArgs("-server").build()).run();

    }

    public static Object v;
    public static long i1;
    public static long i2;
    public static long i3;
    public static long i4;
    public static long i5;
    public static long i6;
    public static long i7;
    public static long i8;
    public static long i9;

    static Map<Class<?>, Runnable> classMapper = new IdentityHashMap<>();
    static ByteArrayList sArrayList = new ByteArrayList();

    static {
        v = Math.random() > 0.5 ? new double[]{} : new float[]{};
        classMapper.put(Byte.class, () -> i1++);
        classMapper.put(Short.class, () -> i2++);
        classMapper.put(Integer.class, () -> i3++);
        classMapper.put(Long.class, () -> i4++);
        classMapper.put(Float.class, () -> i5++);
        classMapper.put(Double.class, () -> i6++);
        classMapper.put(byte[].class, () -> i4++);
        classMapper.put(short[].class, () -> i7++);
        classMapper.put(int[].class, () -> i7++);
        classMapper.put(long[].class, () -> i9++);
        classMapper.put(float[].class, () -> i7++);
        classMapper.put(double[].class, () -> i8++);
        classMapper.put(String.class, () -> i6++);
        classMapper.put(ByteArrayList.class, () -> i2++);
        classMapper.put(ShortArrayList.class, () -> i6++);
        classMapper.put(IntArrayList.class, () -> i3++);
        classMapper.put(List.class, () -> i9++);
    }

    @Benchmark
    public static void testClassMapper() {
        Object v = TestBenchmark.v;
        classMapper.get(v.getClass()).run();
        TestBenchmark.v = Math.random() > 0.5 ? new double[]{} : sArrayList;
    }

    @Benchmark
    public static void testInstanceOf() {
        Object v = TestBenchmark.v;
        if (v instanceof Byte) {
            i1++;
        } else if (v instanceof Short) {
            i2++;
        } else if (v instanceof Integer) {
            i3++;
        } else if (v instanceof Long) {
            i4++;
        } else if (v instanceof Float) {
            i5++;
        } else if (v instanceof Double) {
            i6++;
        } else if (v instanceof byte[] a) {
            i7++;
        } else if (v instanceof short[] a) {
            i8++;
        } else if (v instanceof int[] a) {
            i9++;
        } else if (v instanceof long[] a) {
            i5++;
        } else if (v instanceof float[] a) {
            i7++;
        } else if (v instanceof double[] a) {
            i9++;
        } else if (v instanceof String) {
            i3++;
        } else if (v instanceof ByteArrayList l) {
            i3++;
        } else if (v instanceof ShortArrayList l) {
            i3++;
        } else if (v instanceof IntArrayList l) {
            i6++;
        } else if (v instanceof List<?> l) {
            i8++;
        }
        TestBenchmark.v = Math.random() > 0.5 ? new double[]{} : sArrayList;
    }

    /*public static void testReentrantLock() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        Lock readLock = lock.readLock();

        Int2ObjectOpenHashMap<Object> map = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < 33; i++) {
            map.put(i, new Object());
        }

        for (int i = 0; i < 1000000; i++) {
            readLock.lock();
            try {
                map.get(1);
            } finally {
                readLock.unlock();
            }
        }
    }

    static volatile ConcurrentHashMap<Integer, Object> conMap = new ConcurrentHashMap<>();

    static volatile Int2ObjectMap<Object> map = new Int2ObjectOpenHashMap<>();
    static volatile Int2ObjectMap<Object> syncMap = new Int2ObjectOpenHashMap<>();
    static final Object lock = new Object();

    static {
        for (int i = 0; i < 33; i++) {
            conMap.put(i, new Object());
        }
        for (int i = 0; i < 33; i++) {
            map.put(i, new Object());
        }
        for (int i = 0; i < 33; i++) {
            syncMap.put(i, new Object());
        }
    }

    @Benchmark
    public static void testConcurrentMap() {
        var m = conMap;
        for (int i = 0; i < 1000000; i++) {
            m.get(13);
        }
    }

    @Benchmark
    public static void testSynchronizedMap() {
        var m = syncMap;
        for (int i = 0; i < 1000000; i++) {
            synchronized (m) {
                m.get(13);
            }
        }
    }

    @Benchmark
    public static void testMap() {
        var m = map;
        for (int i = 0; i < 1000000; i++) {
            m.get(13);
        }
    }*/
}
