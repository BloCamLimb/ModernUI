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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;

@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestIterator {

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(TestIterator.class.getSimpleName())
                .jvmArgs("-XX:+UseZGC", "-XX:+ZGenerational")
                .shouldFailOnError(true).shouldDoGC(true)
                .build())
                .run();
    }

    private ArrayList<String> list;
    private ObjectArrayList<String> list2;

    @Setup
    public void setup() {
        list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(String.valueOf(i));
        }
        list2 = new ObjectArrayList<>(list);
    }

    @Benchmark
    public void fori(Blackhole blackhole) {
        for (int i = 0, e = list.size(); i < e; i++) {
            blackhole.consume(list.get(i));
        }
    }

    @Benchmark
    public void foriObject(Blackhole blackhole) {
        for (int i = 0, e = list2.size(); i < e; i++) {
            blackhole.consume(list2.get(i));
        }
    }

    @Benchmark
    public void enhancedFor(Blackhole blackhole) {
        for (var s : list) {
            blackhole.consume(s);
        }
    }

    @Benchmark
    public void enhancedForObject(Blackhole blackhole) {
        for (var s : list2) {
            blackhole.consume(s);
        }
    }

    @Benchmark
    public void arrayObject(Blackhole blackhole) {
        Object[] es = list2.elements();
        for (int i = 0, e = list2.size(); i < e; i++) {
            blackhole.consume(es[i]);
        }
    }
}
