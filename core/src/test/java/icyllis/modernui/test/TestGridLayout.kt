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

import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.GridLayout
import icyllis.modernui.widget.TextView
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.ALL)
    ModernUI().use { app -> app.run(TestGridLayout()) }
}

class TestGridLayout : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val layout = GridLayout(context)
        layout.rowCount = 4
        layout.columnCount = 4
        layout.useDefaultMargins = true
        layout.isRowOrderPreserved = false
        layout.isColumnOrderPreserved = false

        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1F),
            GridLayout.spec(GridLayout.UNDEFINED, 1F)
        )

        layout.addView(text("A"), GridLayout.LayoutParams(params))
        layout.addView(text("B"), GridLayout.LayoutParams(params))
        layout.addView(
            text("Wide"),
            GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1F),
                GridLayout.spec(GridLayout.UNDEFINED, 4, 1F)
            )
        )
        layout.addView(text("C"), GridLayout.LayoutParams(params))
        layout.addView(
            text("Large"),
            GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 2, 1F),
                GridLayout.spec(1, 2, 1F)
            )
        )
        layout.addView(text("D"), GridLayout.LayoutParams(params))
        layout.addView(text("E"), GridLayout.LayoutParams(params))

        return layout
    }

    private fun text(text: String): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.background = ColorDrawable((Math.random() * 0xFFFFFF).toInt() or 0xC0000000.toInt())
        return tv
    }
}