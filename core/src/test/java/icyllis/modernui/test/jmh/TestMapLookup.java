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

import icyllis.modernui.util.ArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;

@Fork(value = 2, jvmArgsAppend = "-XX:+UseFMA")
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestMapLookup {

    //Benchmark                 Mode  Cnt          Score          Error  Units
    //TestMapLookup.arraymap   thrpt   10   97948129.627 ±  4957283.059  ops/s
    //TestMapLookup.hashmap    thrpt   10  317429673.720 ± 45393535.316  ops/s
    //TestMapLookup.mapN       thrpt   10   75012605.703 ±  7640368.507  ops/s
    //TestMapLookup.openhash   thrpt   10  188867396.249 ± 10791847.162  ops/s
    //TestMapLookup.openhash2  thrpt   10  175401127.970 ± 21132512.871  ops/s

    // memory usage: arraymap < openhash < mapN <= openhash2 < hashmap

    private static final HashMap<String, Object> hashmap = new HashMap<>();
    private static final Object2ObjectOpenHashMap<String, Object> openhash = new Object2ObjectOpenHashMap<>(16, 0.75f);
    private static final Object2ObjectOpenHashMap<String, Object> openhash2 = new Object2ObjectOpenHashMap<>(16, 0.5f);
    private static final ArrayMap<String, Object> arraymap = new ArrayMap<>();
    private static final Map<String, Object> mapN; // ImmutableCollections.MapN

    static {
        long val = 7;
        for (int i = 0; i < 1500; i++) {
            String key = Long.toString(val);
            hashmap.put(key, new Object());
            openhash.put(key, new Object());
            openhash2.put(key, new Object());
            arraymap.put(key, new Object());
            val *= 13;
        }
        for (int i = 3500; i < 4500; i += 2) {
            String key = Integer.toString(i);
            hashmap.put(key, new Object());
            openhash.put(key, new Object());
            openhash2.put(key, new Object());
            arraymap.put(key, new Object());
        }
        mapN = Map.copyOf(hashmap);
    }

    @State(Scope.Thread)
    public static class InputState {
        String[] inputs;
        int index = 0;

        @Setup
        public void setUp() {
            inputs = new String[512];
            for (int i = 0; i < 512; i++) {
                inputs[i] = Integer.toString(i + 3200);
            }
        }

        public String get() {
            return inputs[index++ & 511];
        }
    }

    @Benchmark
    public void hashmap(InputState input, Blackhole bh) {
        bh.consume(hashmap.get(input.get()));
    }

    @Benchmark
    public void openhash(InputState input, Blackhole bh) {
        bh.consume(openhash.get(input.get()));
    }

    @Benchmark
    public void openhash2(InputState input, Blackhole bh) {
        bh.consume(openhash2.get(input.get()));
    }

    @Benchmark
    public void arraymap(InputState input, Blackhole bh) {
        bh.consume(arraymap.get(input.get()));
    }

    @Benchmark
    public void mapN(InputState input, Blackhole bh) {
        bh.consume(mapN.get(input.get()));
    }
}
