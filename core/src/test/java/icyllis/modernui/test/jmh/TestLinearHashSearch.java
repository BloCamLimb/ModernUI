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
import org.openjdk.jmh.annotations.*;

import java.nio.file.StandardOpenOption;
import java.util.IdentityHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class TestLinearHashSearch {

    //Benchmark                       (n)  Mode  Cnt        Score        Error  Units
    //TestLinearHashSearch.hash      1000  avgt   10  1291452.464 ± 153879.649  ns/op
    //TestLinearHashSearch.hash      2500  avgt   10  1529542.892 ±  44195.268  ns/op
    //TestLinearHashSearch.hash      4500  avgt   10  1513253.319 ±  65934.175  ns/op
    //TestLinearHashSearch.openhash  1000  avgt   10  1712621.478 ±  27471.244  ns/op
    //TestLinearHashSearch.openhash  2500  avgt   10  1813083.837 ±  48499.369  ns/op
    //TestLinearHashSearch.openhash  4500  avgt   10  1603218.558 ±  74255.394  ns/op

    // HIGH CACHE MISS
    //Benchmark                      (n)  Mode  Cnt        Score        Error  Units
    //TestLinearHashSearch.hash        8  avgt   10  1582421.949 ± 127037.884  ns/op
    //TestLinearHashSearch.hash       16  avgt   10  1466541.503 ± 217239.804  ns/op
    //TestLinearHashSearch.hash       32  avgt   10  1405776.671 ±  82931.993  ns/op
    //TestLinearHashSearch.hash       64  avgt   10  1395701.466 ± 124749.135  ns/op
    //TestLinearHashSearch.linear      8  avgt   10   563959.994 ±  92899.797  ns/op
    //TestLinearHashSearch.linear     16  avgt   10   927096.655 ± 152753.953  ns/op
    //TestLinearHashSearch.linear     32  avgt   10  1516666.036 ± 188861.218  ns/op
    //TestLinearHashSearch.linear     64  avgt   10  2483005.277 ± 187071.818  ns/op
    //TestLinearHashSearch.openhash    8  avgt   10  1712695.938 ±  27818.289  ns/op
    //TestLinearHashSearch.openhash   16  avgt   10  1764028.737 ± 243617.962  ns/op
    //TestLinearHashSearch.openhash   32  avgt   10  1867314.158 ±  20193.544  ns/op
    //TestLinearHashSearch.openhash   64  avgt   10  1835333.742 ±  80396.748  ns/op

    // LOW CACHE MISS
    //Benchmark                      (n)  Mode  Cnt        Score        Error  Units
    //TestLinearHashSearch.hash        8  avgt   10  1090048.904 ± 160992.070  ns/op
    //TestLinearHashSearch.hash       16  avgt   10  1217637.550 ± 213454.922  ns/op
    //TestLinearHashSearch.hash       32  avgt   10  1055237.651 ±  64697.121  ns/op
    //TestLinearHashSearch.hash       64  avgt   10   940151.885 ±  53162.741  ns/op
    //TestLinearHashSearch.linear      8  avgt   10  1153111.663 ±  22744.741  ns/op
    //TestLinearHashSearch.linear     16  avgt   10  1256827.062 ±  48154.149  ns/op
    //TestLinearHashSearch.linear     32  avgt   10  1637180.162 ± 153470.689  ns/op
    //TestLinearHashSearch.linear     64  avgt   10  2306504.444 ± 396512.997  ns/op
    //TestLinearHashSearch.openhash    8  avgt   10   825827.373 ±  18946.014  ns/op
    //TestLinearHashSearch.openhash   16  avgt   10  1194950.775 ±  84758.596  ns/op
    //TestLinearHashSearch.openhash   32  avgt   10   936525.875 ±  35929.045  ns/op
    //TestLinearHashSearch.openhash   64  avgt   10   892653.643 ±  30019.011  ns/op

    @Param({"8", "16", "32", "64"/*, "128", "1000", "2500", "4500"*/})
    public int n;

    private Object[] list;
    private IdentityHashMap<Object, Integer> map;
    private Reference2IntOpenHashMap<Object> openmap;
    private Object[] queries;

    @Setup
    public void setup() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Object[] all = new Object[10000];
        Object[] enums = StandardOpenOption.values();
        for (int i = 0; i < all.length; i++) {
            switch (rnd.nextInt(3)) {
                case 0 -> {
                    all[i] = i * 2 + 1;
                }
                case 1 -> {
                    all[i] = enums[rnd.nextInt(enums.length)];
                }
                default -> {
                    all[i] = new Object();
                }
            }
        }

        list = new Object[n];
        map = new IdentityHashMap<>(n);
        openmap = new Reference2IntOpenHashMap<>(n);
        openmap.defaultReturnValue(-1);
        for (int i = 0; i < n; i++) {
            list[i] = all[i];
            map.put(all[i], i);
            openmap.put(all[i], i);
        }

        queries = new Object[100000];
        for (int i = 0; i < queries.length; i++) {
            if (rnd.nextInt(8) == 0) {
                queries[i] = new Object();
            } else {
                queries[i] = all[rnd.nextInt(n)];
            }
        }
    }

    @Benchmark
    public int linear() {
        int acc = 0;
        for (Object q : queries) {
            int i = list.length - 1;
            for (; i >= 0; i--) {
                if (list[i] == q) {
                    break;
                }
            }
            acc += i;
        }
        return acc;
    }

    @Benchmark
    public int hash() {
        int acc = 0;
        for (Object q : queries) {
            Integer v = map.get(q);
            acc += (v == null) ? -1 : v;
        }
        return acc;
    }

    @Benchmark
    public int openhash() {
        int acc = 0;
        for (Object q : queries) {
            int v = openmap.getInt(q);
            acc += v;
        }
        return acc;
    }
}
