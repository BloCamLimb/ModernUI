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
import icyllis.modernui.view.Menu
import icyllis.modernui.view.MenuItem
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.Toast

fun main() {
    System.setProperty("java.awt.headless", "true")
    Log.setLevel(Log.DEBUG)
    ModernUI().use { app ->
        app.run(TestContextMenu())
    }
}

class TestContextMenu : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val view = View(context)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RING
            gradientType = GradientDrawable.ANGULAR_GRADIENT
            colors = intArrayOf(
                Color.argb(255, 45, 212, 191),
                Color.argb(255, 14, 165, 233)
            )
            setStroke(4, Color.argb(255, 255, 255, 255))
            isDither = true
            level = 9000
            useLevel = true
        }
        view.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val menuItemClickListener = MenuItem.OnMenuItemClickListener {
            // your logic here
            Toast.makeText(view.context, "Clicked item ${it.itemId}", Toast.LENGTH_SHORT)
                .show()
            true
        }
        view.setOnCreateContextMenuListener { menu, _, _ ->
            menu.add(Menu.NONE, 1, Menu.NONE, "Item 1").setOnMenuItemClickListener(menuItemClickListener)
            menu.add(Menu.NONE, 2, Menu.NONE, "Item 2").setOnMenuItemClickListener(menuItemClickListener)
            menu.add(Menu.NONE, 3, Menu.NONE, "Item 3").setOnMenuItemClickListener(menuItemClickListener)
            menu.add(Menu.NONE, 4, Menu.NONE, "Item 4").setOnMenuItemClickListener(menuItemClickListener)
            menu.add(Menu.NONE, 5, Menu.NONE, "Item 5").setOnMenuItemClickListener(menuItemClickListener)
            menu.add(Menu.NONE, 6, Menu.NONE, "Item 6").setOnMenuItemClickListener(menuItemClickListener)
        }
        return view
    }
}