/*
 * ModernUI.
 * Copyright (C) 2024-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.test

import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Color
import icyllis.modernui.graphics.drawable.GradientDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.HorizontalScrollView

fun main() {
    System.setProperty("java.awt.headless", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    ModernUI().use { app -> app.run(TestHorizontalScrollView()) }
}

class TestHorizontalScrollView : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val sv = HorizontalScrollView(context)

        val content = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setColors(intArrayOf(
                    Color.argb(255, 45, 212, 191),
                    Color.argb(255, 14, 165, 233)
                ))
                setStroke(4, Color.argb(255, 255, 255, 255))
                isDither = true
            }
        }
        content.minimumWidth = sv.dp(1000f)
        content.layoutParams =
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        sv.addView(content)

        val params = FrameLayout.LayoutParams(sv.dp(640f), sv.dp(360f))
        params.gravity = Gravity.CENTER
        sv.setLayoutParams(params)
        return sv
    }
}