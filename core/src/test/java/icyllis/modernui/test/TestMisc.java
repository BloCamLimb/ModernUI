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

import com.ibm.icu.text.CompactDecimalFormat;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.AlgorithmUtils;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.Log;
import org.lwjgl.system.MemoryUtil;

import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;

public class TestMisc {

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        var complex = TypedValue.floatToComplex(-3.6f);
        Log.LOGGER.info(
                "Complex to Float: {} -> {}",
                Integer.toHexString(complex),
                TypedValue.complexToFloat(complex)
        );
        var format = CompactDecimalFormat.getInstance(
                new Locale("zh"), CompactDecimalFormat.CompactStyle.SHORT
        );
        format.setMaximumFractionDigits(2);
        Log.LOGGER.info(format.format(new BigDecimal("2136541565.615")));
        Log.LOGGER.info("Levenshtein distance: {}", TextUtils.distance("sunday", "saturday"));

        var doubles = new double[]{1.0, 160.0, 3.0};
        Arrays.stream(doubles).average().ifPresent(d -> Log.LOGGER.info(Double.toString(d)));
        Log.LOGGER.info(Double.toString(AlgorithmUtils.averageStable(doubles)));

        var ptr = MemoryUtil.nmemAlloc(4);
        MemoryUtil.memPutByte(ptr + 0, (byte) 0x11);
        MemoryUtil.memPutByte(ptr + 1, (byte) 0x22);
        MemoryUtil.memPutByte(ptr + 2, (byte) 0x33);
        MemoryUtil.memPutByte(ptr + 3, (byte) 0x44);

        Log.LOGGER.info(
                "{} {}",
                ByteOrder.nativeOrder(),
                Integer.toHexString(MemoryUtil.memGetInt(ptr))
        );
    }
}
