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
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

@Fork(value = 2, jvmArgsAppend = "-XX:+UseFMA")
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestMapLookup {

    private static final HashMap<String, Object> hashmap = new HashMap<>();
    private static final Object2ObjectOpenHashMap<String, Object> openhash = new Object2ObjectOpenHashMap<>();
    private static final ArrayMap<String, Object> arraymap = new ArrayMap<>();
    private static final Random rand = new Random();

    static {
        for (int i = 0; i < 1500; i++) {
            String key = UUID.randomUUID().toString();
            hashmap.put(key, new Object());
            openhash.put(key, new Object());
            arraymap.put(key, new Object());
        }
        for (int i = 500; i < 1500; i += 2) {
            String key = Integer.toString(i);
            hashmap.put(key, new Object());
            openhash.put(key, new Object());
            arraymap.put(key, new Object());
        }
    }

    @Benchmark
    public void hashmap(Blackhole bh) {
        bh.consume(hashmap.get(Integer.toString(rand.nextInt(200, 728))));
    }

    @Benchmark
    public void openhash(Blackhole bh) {
        bh.consume(openhash.get(Integer.toString(rand.nextInt(200, 728))));
    }

    @Benchmark
    public void arraymap(Blackhole bh) {
        bh.consume(arraymap.get(Integer.toString(rand.nextInt(200, 728))));
    }
}
