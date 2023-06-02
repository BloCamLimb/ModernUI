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
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.View.OnFocusChangeListener
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.viewpager.widget.LinearPagerIndicator
import icyllis.modernui.viewpager.widget.PagerAdapter
import icyllis.modernui.viewpager.widget.ViewPager
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.TextView
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.ALL)
    ModernUI().use { app -> app.run(TestViewPager()) }
}

class TestViewPager : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val pager = ViewPager(context)
        pager.adapter = MyAdapter()
        pager.isFocusableInTouchMode = true
        pager.isKeyboardNavigationCluster = true

        // press 'tab' key to take focus, and use arrow keys
        pager.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            ModernUI.LOGGER.info(
                "{} focus change: {}",
                v,
                hasFocus
            )
        }
        run {
            val indicator = LinearPagerIndicator(context)
            indicator.setPager(pager)
            val lp = ViewPager.LayoutParams()
            lp.height = pager.dp(30f)
            lp.isDecor = true
            lp.gravity = Gravity.CENTER_HORIZONTAL
            pager.addView(indicator, lp)
        }
        val lp = FrameLayout.LayoutParams(pager.dp(480f), pager.dp(360f))
        lp.gravity = Gravity.CENTER
        pager.layoutParams = lp

        // used when pages change too fast
        //pager.setOffscreenPageLimit(2);
        return pager
    }

    internal class MyAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return 7
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val tv = TextView(container.context)
            tv.text = "This is page $position"
            tv.gravity = Gravity.CENTER
            container.addView(tv)
            return tv
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }
    }
}
