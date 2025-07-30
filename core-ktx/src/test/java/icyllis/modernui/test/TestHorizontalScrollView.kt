/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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
import icyllis.modernui.TestFragment.TestLinearLayout
import icyllis.modernui.fragment.Fragment
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

        val content = TestLinearLayout(context)
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