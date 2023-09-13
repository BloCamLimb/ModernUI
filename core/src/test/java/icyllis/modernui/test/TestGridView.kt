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
import icyllis.modernui.graphics.text.FontFamily
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.ArrayAdapter
import icyllis.modernui.widget.GridView
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.ALL)
    ModernUI().use { app -> app.run(TestGridView()) }
}

class TestGridView : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val view = GridView(context)
        view.numColumns = 3
        val list = ArrayList(FontFamily.getSystemFontMap().keys)
        list.sort()
        view.adapter = ArrayAdapter(context, list)

        return view
    }
}