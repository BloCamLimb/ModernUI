/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

@Fork(2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestArrayCopy {

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(TestArrayCopy.class.getSimpleName())
                .shouldFailOnError(true).shouldDoGC(true)
                .build())
                .run();
    }

    public static final Random RANDOM = new Random();

    @Benchmark
    public static void manualLoop(Blackhole blackhole) {
        float[] src = {RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()};
        float[] dst = new float[4];
        for (int i = 0; i < 4; i++) {
            dst[i] = src[i];
        }
        blackhole.consume(dst);
    }

    @Benchmark
    public static void arrayCopy(Blackhole blackhole) {
        float[] src = {RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()};
        float[] dst = new float[4];
        System.arraycopy(src, 0, dst, 0, 4);
        blackhole.consume(dst);
    }

    @Benchmark
    public static void plain(Blackhole blackhole) {
        float[] src = {RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat()};
        float[] dst = new float[4];
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
        blackhole.consume(dst);
    }
}
