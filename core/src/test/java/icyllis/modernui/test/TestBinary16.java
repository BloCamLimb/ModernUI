/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.modernui.util.Half;

public class TestBinary16 {

    //-XX:+UnlockDiagnosticVMOptions
    //-XX:+UnlockExperimentalVMOptions
    //-XX:CompileCommand="print *Half::*"
    //-XX:-TieredCompilation
    // compile hsdis
    public static void main(String[] args) {
        for (int i = 0; i < 10000000; i++) {
            Half.toFloat((short) i);
        }
        for (int i = 0; i < 20; i++) {
            System.out.println("Hello hello");
        }
        for (int i = 0; i < 10000000; i++) {
            Half.toFloat((short) i);
        }
    }
}
