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

package icyllis.modernui.test

import icyllis.modernui.ModernUI
import icyllis.modernui.R
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.drawable.BuiltinIconDrawable
import icyllis.modernui.graphics.drawable.ShapeDrawable
import icyllis.modernui.resources.TypedValue
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT
import icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TabLayout

fun main() {
    System.setProperty("java.awt.headless", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    ModernUI().use { app ->
        app.run(TestTabLayout())
    }
}

class TestTabLayout : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = ShapeDrawable().apply {
                shape = ShapeDrawable.HLINE
                setSize(-1, dp(1F))
                val value = TypedValue()
                context.theme.resolveAttribute(R.ns, R.attr.colorOutlineVariant, value, true)
                setColor(value.data)
            }
            addView(TabLayout(requireContext()).apply {
                addTab(newTab().apply {
                    text = "Home"
                    icon = BuiltinIconDrawable(context.resources, BuiltinIconDrawable.KEYBOARD_ARROW_LEFT)
                })
                addTab(newTab().apply {
                    text = "Trips"
                    icon = BuiltinIconDrawable(context.resources, BuiltinIconDrawable.KEYBOARD_ARROW_UP)
                })
                addTab(newTab().apply {
                    text = "Flights"
                    icon = BuiltinIconDrawable(context.resources, BuiltinIconDrawable.KEYBOARD_ARROW_RIGHT)
                })
                addTab(newTab().apply {
                    text = "Explore"
                    icon = BuiltinIconDrawable(context.resources, BuiltinIconDrawable.KEYBOARD_ARROW_DOWN)
                })
                tabGravity = TabLayout.GRAVITY_CENTER
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    Gravity.CENTER
                }
            })
            addView(TabLayout(requireContext(), null, R.attr.tabSecondaryStyle).apply {
                addTab(newTab().apply {
                    text = "Overview"
                })
                addTab(newTab().apply {
                    text = "Ingredients"
                })
                addTab(newTab().apply {
                    text = "Instructions"
                })
                addTab(newTab().apply {
                    text = "Specifications"
                })
                tabGravity = TabLayout.GRAVITY_CENTER
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    Gravity.CENTER
                }
            })
        }
    }
}