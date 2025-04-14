/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2025 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Fork(2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class FieldAccessBenchmark {

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(FieldAccessBenchmark.class.getSimpleName())
                .jvmArgs("-XX:+UseZGC", "-XX:+ZGenerational")
                .shouldFailOnError(true).shouldDoGC(true)
                .build())
                .run();
    }

    @State(Scope.Thread)
    public static class MyState {
        public final Rect2f r1 = new Rect2f(60, 70, 80, 90);
        public final Rect2f r2 = new Rect2f(50, 70, 60, 90);
    }

    @Benchmark
    public static void direct(MyState state, Blackhole bh) {
        var r = state.r2;
        bh.consume(state.r1.intersects(r.left(), r.top(), r.right(), r.bottom()));
    }

    // this won
    @Benchmark
    public static void viaInterface(MyState state, Blackhole bh) {
        bh.consume(state.r1.intersects(state.r2));
    }
}
