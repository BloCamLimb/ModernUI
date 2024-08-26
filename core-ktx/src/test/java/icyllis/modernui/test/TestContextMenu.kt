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

package icyllis.modernui.test

import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Color
import icyllis.modernui.graphics.drawable.GradientDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.util.Log
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup

fun main() {
    System.setProperty("java.awt.headless", "true")
    Log.setLevel(Log.DEBUG)
    ModernUI().use { app -> app.run(TestContextMenu()) }
}

class TestContextMenu : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val view = View(context)
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RING
        drawable.gradientType = GradientDrawable.ANGULAR_GRADIENT
        drawable.colors = intArrayOf(
            Color.argb(255, 45, 212, 191),
            Color.argb(255, 14, 165, 233)
        )
        drawable.setStroke(4, Color.argb(255, 255, 255, 255))
        drawable.isDither = true
        drawable.level = 9000
        drawable.useLevel = true
        view.background = drawable
        view.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add("1")
            menu.add("2")
            menu.add("3")
            menu.add("4")
            menu.add("5")
            menu.add("6")
        }
        return view
    }
}