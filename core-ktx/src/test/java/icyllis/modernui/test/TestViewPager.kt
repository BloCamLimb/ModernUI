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
import icyllis.modernui.R
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.graphics.drawable.ShapeDrawable
import icyllis.modernui.resources.TypedValue
import icyllis.modernui.util.DataSet
import icyllis.modernui.util.Log
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.View.OnFocusChangeListener
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT
import icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT
import icyllis.modernui.widget.*

fun main() {
    System.setProperty("java.awt.headless", "true")
    Log.setLevel(Log.DEBUG)
    ModernUI().use { app ->
        app.run(TestViewPager())
    }
}

class TestViewPager : Fragment() {
    private lateinit var pager: ViewPager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        container!!.layoutDirection = View.LAYOUT_DIRECTION_RTL
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(View(context).apply {
                val divider = ShapeDrawable()
                divider.shape = ShapeDrawable.HLINE
                divider.setSize(-1, dp(1F))
                val value = TypedValue()
                context.theme.resolveAttribute(R.ns, R.attr.colorOutlineVariant, value, true)
                divider.setColor(value.data)
                background = divider
            }, LinearLayout.LayoutParams(dp(960F), dp(1F)))
            addView(ViewPager(context).apply {
                pager = this
                adapter = MyAdapter()
                isFocusableInTouchMode = true
                isKeyboardNavigationCluster = true
                // press 'tab' key to take focus, and use arrow keys
                onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                    Log.info(null, "{} focus change: {}", v, hasFocus)
                }
                /*addView(TabLayout(context).apply {
                    tabMode = TabLayout.MODE_AUTO
                }, ViewPager.LayoutParams().apply {
                    width = MATCH_PARENT
                    height = WRAP_CONTENT
                    gravity = Gravity.TOP
                })*/
                setPadding(dp(8F), 0, dp(8F), 0)
                pageMargin = dp(16F)
            }, LinearLayout.LayoutParams(dp(960F), dp(540F)).apply {
                gravity = Gravity.CENTER
            })
            addView(TabLayout(context).apply {
                setupWithViewPager(pager)
                tabMode = TabLayout.MODE_AUTO
                layoutParams = LinearLayout.LayoutParams(dp(960F), WRAP_CONTENT)
            }, 0)
            addView(Button(context, null, R.attr.buttonOutlinedStyle).apply {
                text = "Increment count"
                setOnClickListener {
                    (pager.adapter as MyAdapter).apply {
                        val curItem = pager.currentItem
                        count++
                        notifyDataSetChanged()
                        val nextItem = curItem + 1
                        if (nextItem < count) {
                            Log.info(null, "Set item to {}", nextItem)
                            pager.currentItem = nextItem
                        }
                    }
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                setMargins(0, dp(4F), 0, dp(4F))
            })
            addView(Button(context, null, R.attr.buttonOutlinedStyle).apply {
                text = "Decrement count"
                setOnClickListener {
                    (pager.adapter as MyAdapter).apply {
                        count--
                        notifyDataSetChanged()
                    }
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                setMargins(0, dp(4F), 0, dp(4F))
            })
            addView(ToggleButton(context, null, R.attr.buttonOutlinedStyle).apply {
                text = "Toggle LTR/RTL"
                setOnCheckedChangeListener { _, checked ->
                    (pager.parent as? View)?.apply {
                        layoutDirection = if (checked) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                    }
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                setMargins(0, dp(4F), 0, dp(4F))
            })
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    internal class MyAdapter : PagerAdapter() {
        @JvmField
        var count: Int = 7

        override fun getCount(): Int {
            return count
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            Log.info(null, "Instantiate item {}", position)
            return TextView(container.context).apply {
                text = "This is page $position"
                tag = position
                gravity = Gravity.CENTER
                val value = TypedValue()
                context.theme.resolveAttribute(R.ns, R.attr.colorSurface, value, true)
                background = ColorDrawable(value.data)
            }.also {
                container.addView(it, 0)
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            Log.info(null, "Destroy item {}", position)
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getItemPosition(`object`: Any): Int {
            val tv = `object` as TextView
            val pos = tv.tag as Int
            if (pos < count)
                return pos
            return POSITION_NONE
        }

        /*override fun getPageWidth(position: Int): Float {
            return if (position and 1 == 0)
                1.0F
            else 0.5F
        }*/

        override fun getPageTitle(position: Int): CharSequence {
            return "Page $position"
        }
    }
}
