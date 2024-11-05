/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jni.JNINativeInterface;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;
import sun.misc.*;

import java.util.concurrent.TimeUnit;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.libc.LibCString.*;

@State(Scope.Benchmark)
public class TestMemCpy {

    private static final Unsafe UNSAFE = getUnsafe();

    private static sun.misc.Unsafe getUnsafe() {
        try {
            var field = MemoryUtil.class.getDeclaredField("UNSAFE");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError("No MemoryUtil.UNSAFE", e);
        }
    }

    //                                                              lower is better
    // Benchmark                                (length)  Mode  Cnt   Score    Error  Units
    // TestMemCpy.array_baseline                      32  avgt    3   3.320 ±  0.179  ns/op
    // TestMemCpy.array_baseline                     160  avgt    3   3.796 ±  0.296  ns/op
    // TestMemCpy.array_baseline                     256  avgt    3   5.236 ±  8.546  ns/op
    // TestMemCpy.array_baseline                    1024  avgt    3  19.374 ± 61.547  ns/op
    // TestMemCpy.array_to_offheap_base_region        32  avgt    3  23.368 ±  2.762  ns/op
    // TestMemCpy.array_to_offheap_base_region       160  avgt    3  27.423 ±  2.958  ns/op
    // TestMemCpy.array_to_offheap_base_region       256  avgt    3  28.233 ±  2.385  ns/op
    // TestMemCpy.array_to_offheap_base_region      1024  avgt    3  30.137 ± 12.089  ns/op
    // TestMemCpy.array_to_offheap_baseline           32  avgt    3   9.017 ±  0.445  ns/op
    // TestMemCpy.array_to_offheap_baseline          160  avgt    3  10.124 ±  0.582  ns/op
    // TestMemCpy.array_to_offheap_baseline          256  avgt    3  11.016 ±  0.364  ns/op
    // TestMemCpy.array_to_offheap_baseline         1024  avgt    3  15.842 ±  8.593  ns/op
    // TestMemCpy.offheap_LWJGL                       32  avgt    3   2.667 ±  0.088  ns/op
    // TestMemCpy.offheap_LWJGL                      160  avgt    3  10.137 ±  0.747  ns/op
    // TestMemCpy.offheap_LWJGL                      256  avgt    3  10.948 ±  0.608  ns/op
    // TestMemCpy.offheap_LWJGL                     1024  avgt    3  16.164 ±  1.226  ns/op
    // TestMemCpy.offheap_baseline                    32  avgt    3   9.304 ±  0.934  ns/op
    // TestMemCpy.offheap_baseline                   160  avgt    3  10.039 ±  0.859  ns/op
    // TestMemCpy.offheap_baseline                   256  avgt    3  11.081 ±  0.517  ns/op
    // TestMemCpy.offheap_baseline                  1024  avgt    3  16.533 ±  1.473  ns/op
    // TestMemCpy.offheap_java                        32  avgt    3   1.935 ±  0.067  ns/op
    // TestMemCpy.offheap_java                       160  avgt    3   6.704 ±  0.366  ns/op
    // TestMemCpy.offheap_java                       256  avgt    3   9.278 ±  0.023  ns/op
    // TestMemCpy.offheap_java                      1024  avgt    3  31.557 ± 33.803  ns/op
    // TestMemCpy.offheap_libc                        32  avgt    3   7.455 ±  0.613  ns/op
    // TestMemCpy.offheap_libc                       160  avgt    3   8.135 ±  0.481  ns/op
    // TestMemCpy.offheap_libc                       256  avgt    3   8.861 ±  1.035  ns/op
    // TestMemCpy.offheap_libc                      1024  avgt    3  13.589 ±  1.059  ns/op
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TestMemCpy.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1))
                .warmupTime(TimeValue.seconds(1))
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .shouldFailOnError(true).shouldDoGC(true)
                .jvmArgs("-XX:+UseZGC", "-XX:+ZGenerational")
                .build();

        new Runner(opt).run();
    }

    private static final int BUFFER_SIZE = 128 * 1024;

    private static final long f = nmemAlloc(BUFFER_SIZE);
    private static final long t = nmemAlloc(BUFFER_SIZE);

    private static final byte[] a = new byte[BUFFER_SIZE];
    private static final byte[] b = new byte[BUFFER_SIZE];

    @Param({"32", "160", "256", "1024"})
    public int length;

    @Benchmark
    public void offheap_LWJGL() {
        memCopy(f, t, length);
    }

    @Benchmark
    public void offheap_baseline() {
        UNSAFE.copyMemory(null, f, null, t, length);
    }

    @Benchmark
    public void array_to_offheap_baseline() {
        UNSAFE.copyMemory(a, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, t, length);
    }

    @Benchmark
    public void array_to_offheap_base_region() {
        JNINativeInterface.nGetByteArrayRegion(a, 0, length, t);
    }

    @Benchmark
    public void offheap_java() {
        memCopyAligned(f, t, length);
    }

    @Benchmark
    public void offheap_libc() {
        nmemcpy(t, f, length);
    }

    @Benchmark
    public void array_baseline() {
        System.arraycopy(a, 0, b, 0, length);
    }

    private static void memCopyAligned(long src, long dst, int bytes) {
        int aligned = bytes & ~7;

        // Aligned body
        for (int i = 0; i < aligned; i += 8) {
            UNSAFE.putLong(null, dst + i, UNSAFE.getLong(null, src + i));
        }

        // Unaligned tail
        for (int i = aligned; i < bytes; i++) {
            UNSAFE.putByte(null, dst + i, UNSAFE.getByte(null, src + i));
        }
    }
}
