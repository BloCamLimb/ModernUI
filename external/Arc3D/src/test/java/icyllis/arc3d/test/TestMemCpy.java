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
