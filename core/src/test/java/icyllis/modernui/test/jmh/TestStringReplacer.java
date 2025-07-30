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
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Fork(value = 2)
@Threads(2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Thread)
public class TestStringReplacer {

    //Benchmark                           Mode  Cnt        Score        Error  Units
    //TestStringReplacer.direct_replace  thrpt   10  4310939.506 ± 528456.712  ops/s
    //TestStringReplacer.single_edit     thrpt   10  6258261.984 ± 494698.899  ops/s

    private static final
    Pattern PATTERN = Pattern.compile("app?le");

    @Benchmark
    public void single_edit(Blackhole bh) {
        StringBuilder sb = new StringBuilder("apple banana aple cherry apple");
        Matcher matcher = PATTERN.matcher(sb);

        IntArrayList matches = new IntArrayList();
        while (matcher.find()) {
            matches.add(matcher.start());
            matches.add(matcher.end());
        }

        for (int i = matches.size() - 2; i >= 0; i -= 2) {
            int start = matches.getInt(i);
            int end = matches.getInt(i + 1);
            sb.replace(start, end, "orange");
        }

        bh.consume(sb);
    }

    @Benchmark
    public void direct_replace(Blackhole bh) {
        String sb = "apple banana aple cherry apple";
        bh.consume(PATTERN.matcher(sb).replaceAll("orange"));
    }
}
