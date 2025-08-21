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
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT
import icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.NestedScrollView
import icyllis.modernui.widget.ScrollView
import icyllis.modernui.widget.TextView

fun main() {
    System.setProperty("java.awt.headless", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    ModernUI().use { app -> app.run(TestNestedScrollView()) }
}

class TestNestedScrollView : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val context = requireContext();
        val outer = ScrollView(context).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = "Long text."
                    background = ColorDrawable(-0x7f7f7f80)
                }, MATCH_PARENT, dp(600F))
                addView(NestedScrollView(context).apply {
                    addView(TextView(context).apply {
                        text = generateLongText()
                        background = ColorDrawable(-0x1)
                    }, MATCH_PARENT, WRAP_CONTENT)
                }, MATCH_PARENT, dp(600F))
            }, MATCH_PARENT, WRAP_CONTENT)
        }
        return outer
    }

    companion object {
        fun generateLongText(): String {
            val sb = StringBuilder()
            for (i in 1..5) {
                for (j in 1..10) {
                    for (k in 1..j) {
                        sb.append("long ")
                    }
                    sb.replace(sb.length - 1, sb.length, "\n")
                }
                for (j in 10 downTo 1) {
                    for (k in 1..j) {
                        sb.append("long ")
                    }
                    sb.replace(sb.length - 1, sb.length, "\n")
                }
            }
            sb.deleteCharAt(sb.length - 1)
            return sb.toString()
        }
    }
}