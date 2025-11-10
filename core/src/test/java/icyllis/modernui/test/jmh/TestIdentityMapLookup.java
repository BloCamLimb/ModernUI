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

import java.nio.file.StandardOpenOption;
import java.util.IdentityHashMap;
import java.util.concurrent.ThreadLocalRandom;

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

    // HIGH CACHE MISS
    //Benchmark                            Mode  Cnt          Score         Error  Units
    //TestIdentityMapLookup.identityhash  thrpt   25   93191811.469 ± 5218619.263  ops/s
    //TestIdentityMapLookup.openhash      thrpt   25   88379105.586 ± 2202491.721  ops/s
    //TestIdentityMapLookup.openhash2     thrpt   25   86245441.732 ± 2724354.998  ops/s

    // LOW CACHE MISS
    //Benchmark                            Mode  Cnt          Score          Error  Units
    //TestIdentityMapLookup.identityhash  thrpt   25  120794197.188 ±  6584672.540  ops/s
    //TestIdentityMapLookup.openhash      thrpt   25  100437372.206 ±  3853525.948  ops/s
    //TestIdentityMapLookup.openhash2     thrpt   25  123727982.981 ± 11160165.333  ops/s

    // memory usage: openhash < openhash2 < identityhash

    private final IdentityHashMap<Object, Integer> identityhash = new IdentityHashMap<>();
    private final Reference2IntOpenHashMap<Object> openhash = new Reference2IntOpenHashMap<>(16, 0.75f);
    private final Reference2IntOpenHashMap<Object> openhash2 = new Reference2IntOpenHashMap<>(16, 2/3f);

    Object[] inputs;
    int index = 0;

    @Setup
    public void setup() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Object[] all = new Object[10000];
        Object[] enums = StandardOpenOption.values();
        for (int i = 0; i < all.length; i++) {
            switch (rnd.nextInt(3)) {
                case 0 -> {
                    all[i] = Integer.toString(i * 2 + 1).intern();
                }
                case 1 -> {
                    all[i] = enums[rnd.nextInt(enums.length)];
                }
                default -> {
                    all[i] = new Object();
                }
            }
        }

        for (int i = 0; i < 2000; i++) {
            Object key = all[i];
            identityhash.put(key, i);
            openhash.put(key, i);
            openhash2.put(key, i);
        }

        inputs = new Object[100000];
        for (int i = 0; i < inputs.length; i++) {
            if (rnd.nextInt(8) == 0) {
                inputs[i] = new Object();
            } else {
                inputs[i] = all[rnd.nextInt(2000)];
            }
        }
    }

    public Object get() {
        return inputs[index++ % 100000];
    }

    @Benchmark
    public void identityhash(Blackhole bh) {
        Integer val = identityhash.get(get());
        bh.consume(val != null ? val : -1);
    }

    @Benchmark
    public void openhash(Blackhole bh) {
        bh.consume(openhash.getInt(get()));
    }

    @Benchmark
    public void openhash2(Blackhole bh) {
        bh.consume(openhash2.getInt(get()));
    }
}
