/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test

import com.ibm.icu.text.CompactDecimalFormat
import icyllis.modernui.ModernUI
import icyllis.modernui.graphics.MathUtil
import icyllis.modernui.text.TextUtils
import icyllis.modernui.util.TypedValue
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.math.BigDecimal
import java.util.*

fun main() {
    Configurator.setRootLevel(Level.ALL)
    val complex = TypedValue.floatToComplex(-3.6f)
    ModernUI.LOGGER.info(
        "Complex to Float: {} -> {}",
        Integer.toHexString(complex),
        TypedValue.complexToFloat(complex)
    )
    val format = CompactDecimalFormat.getInstance(
        Locale("zh"), CompactDecimalFormat.CompactStyle.SHORT
    )
    format.maximumFractionDigits = 2
    ModernUI.LOGGER.info(format.format(BigDecimal("2136541565.615")))
    ModernUI.LOGGER.info("Levenshtein distance: {}", TextUtils.distance("sunday", "saturday"))

    val doubles = doubleArrayOf(1.0, 160.0, 3.0)
    Arrays.stream(doubles).average().ifPresent { average -> ModernUI.LOGGER.info(average) }
    ModernUI.LOGGER.info(MathUtil.averageStable(doubles))
}
