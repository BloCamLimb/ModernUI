/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.*;

import java.util.Random;

public class CF1840G {

    public static final int TEST_N = 993352;
    public static final IntList TEST_DATA = new IntArrayList(TEST_N);
    public static int sTestDataIndex = 0;
    public static int sNumQueries = 0;

    static {
        for (int i = 0; i < TEST_N; i++) {
            TEST_DATA.add(i + 1);
        }
        IntLists.shuffle(TEST_DATA, new Random());
    }

    public static int query(int delta) {
        sNumQueries++;
        return TEST_DATA.getInt(sTestDataIndex = (sTestDataIndex + delta) % TEST_N);
    }

    public static void main(String[] args) {
        System.out.println(solve());
        System.out.printf("Number of queries: %d\n", sNumQueries);
    }

    public static int solve() {
        var random = new Random();
        var map = new Int2IntOpenHashMap(333, Hash.FAST_LOAD_FACTOR);
        map.defaultReturnValue(-1);
        int max = query(0), pos;
        for (int i = 1; i <= 333; i++)
            max = Math.max(max, query(random.nextInt(1000000)));
        for (int i = 1; i <= 333; i++)
            if (map.put(query(1), max + 333 - i) != -1)
                return i;
        if ((pos = map.get(query(max))) != -1)
            return pos;
        for (int i = 1; i <= 333; i++)
            if ((pos = map.get(query(333))) != -1)
                return i * 333 + pos;
        throw new RuntimeException();
    }
}
