/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test.jmh;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestFiniteCheck {

    //

    float[] inputs;
    int index = 0;

    @Setup
    public void setUp() {
        inputs = new float[512];
        for (int i = 0; i < 512; i++) {
            if (Math.random() < 0.1) {
                inputs[i] = switch (i % 3) {
                    case 0 -> Float.POSITIVE_INFINITY;
                    case 1 -> Float.NEGATIVE_INFINITY;
                    default -> Float.NaN;
                };
            } else {
                inputs[i] = (float) (Math.random() * Float.MAX_VALUE);
            }
        }
    }

    public float get() {
        return inputs[index++ & 511];
    }

    @Benchmark
    public void check_one(Blackhole bh) {
        float v0 = get(), v1 = get(), v2 = get(), v3 = get();
        float prod = v0 - v0;
        prod = prod * v1 * v2 * v3;
        // At this point, `prod` will either be NaN or 0.
        bh.consume(prod == prod);
    }

    @Benchmark
    public void check_two(Blackhole bh) {
        float v0 = get(), v1 = get(), v2 = get(), v3 = get();
        bh.consume(Float.isFinite(v0)
                && Float.isFinite(v1)
                && Float.isFinite(v2)
                && Float.isFinite(v3));
    }

    static final int EXP_MASK = 0x7f800000;

    @Benchmark
    public void check_three(Blackhole bh) {
        float v0 = get(), v1 = get(), v2 = get(), v3 = get();
        bh.consume(((Float.floatToRawIntBits(v0) & EXP_MASK) != EXP_MASK)
                & ((Float.floatToRawIntBits(v1) & EXP_MASK) != EXP_MASK)
                & ((Float.floatToRawIntBits(v2) & EXP_MASK) != EXP_MASK)
                & ((Float.floatToRawIntBits(v3) & EXP_MASK) != EXP_MASK));
    }
}
