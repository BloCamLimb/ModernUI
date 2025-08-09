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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.IdentityHashMap;

@Fork(5)
@Threads(2)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Thread)
public class TestIdentityMapLookup {

    //Benchmark                            Mode  Cnt          Score          Error  Units
    //TestIdentityMapLookup.identityhash  thrpt   25  347170371.504 ±  7449469.836  ops/s
    //TestIdentityMapLookup.openhash      thrpt   25  330088237.990 ± 14930444.763  ops/s
    //TestIdentityMapLookup.openhash2     thrpt   25  307558052.906 ± 19373608.936  ops/s

    // memory usage: openhash < openhash2 < identityhash

    private static final IdentityHashMap<String, Integer> identityhash = new IdentityHashMap<>();
    private static final Reference2IntOpenHashMap<String> openhash = new Reference2IntOpenHashMap<>(16, 0.75f);
    private static final Reference2IntOpenHashMap<String> openhash2 = new Reference2IntOpenHashMap<>(16, 2/3f);

    static {
        long val = 7;
        for (int i = 0; i < 1500; i++) {
            String key = Long.toString(val);
            identityhash.put(key, i);
            openhash.put(key, i);
            openhash2.put(key, i);
            val *= 13;
        }
        for (int i = 3500; i < 4500; i += 2) {
            String key = Integer.toString(i).intern();
            identityhash.put(key, i);
            openhash.put(key, i);
            openhash2.put(key, i);
        }
    }

    @State(Scope.Thread)
    public static class InputState {
        String[] inputs;
        int index = 0;

        @Setup
        public void setUp() {
            inputs = new String[512];
            for (int i = 0; i < 512; i++) {
                inputs[i] = Integer.toString(i + 3200).intern();
            }
        }

        public String get() {
            return inputs[index++ & 511];
        }
    }

    @Benchmark
    public void identityhash(InputState input, Blackhole bh) {
        Integer val = identityhash.get(input.get());
        bh.consume(val != null ? val : -1);
    }

    @Benchmark
    public void openhash(InputState input, Blackhole bh) {
        bh.consume(openhash.getInt(input.get()));
    }

    @Benchmark
    public void openhash2(InputState input, Blackhole bh) {
        bh.consume(openhash2.getInt(input.get()));
    }
}
