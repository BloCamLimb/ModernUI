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

import icyllis.arc3d.core.Color
import icyllis.modernui.ModernUI
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.drawable.toDrawable
import icyllis.modernui.util.DataSet
import icyllis.modernui.util.Log
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT
import icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT
import icyllis.modernui.widget.AbsListView
import icyllis.modernui.widget.BaseAdapter
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.GridView
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ListView
import icyllis.modernui.widget.TextView
import java.lang.ref.Cleaner
import java.lang.ref.Reference
import kotlin.system.exitProcess

fun main() {
    System.setProperty("java.awt.headless", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    ModernUI().use { app -> app.run(TestListViewDetach()) }
}

class TestListViewDetach : Fragment() {

    private lateinit var layout: ViewGroup
    private lateinit var spacer: View
    private lateinit var list: AbsListView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: DataSet?): View {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            addView(
                Button(context, null).apply {
                    text = "Resize list"
                    setOnClickListener {
                        if (spacer.height == 0) {
                            spacer.layoutParams.height = (spacer.parent as View).height / 2
                        } else {
                            spacer.layoutParams.height = 0
                        }
                        spacer.requestLayout()
                        Log.LOGGER.info("List resized")
                        it.postDelayed({ Runtime.getRuntime().gc() }, 1000L)
                    }
                },
                MATCH_PARENT, WRAP_CONTENT
            )

            addView(
                Button(context, null).apply {
                    text = "Remove list"
                    setOnClickListener {
                        if (list.isAttachedToWindow) {
                            this@TestListViewDetach.layout.removeView(list)
                        } else {
                            this@TestListViewDetach.layout.addView(list)
                        }
                    }
                },
                MATCH_PARENT, WRAP_CONTENT
            )

            addView(
                View(context).also { spacer = it },
                MATCH_PARENT, 0
            )

            addView(
                object : GridView(context) {
                    val added = HashSet<View>()
                    override fun onViewAdded(child: View) {
                        super.onViewAdded(child)
                        Log.i(null, "View ${child.tag} added")
                        if (!added.add(child)) {
                            throw AssertionError()
                        }
                    }

                    override fun onViewRemoved(child: View) {
                        super.onViewRemoved(child)
                        Log.i(null, "View ${child.tag} removed")
                        if (!added.remove(child)) {
                            throw AssertionError()
                        }
                    }
                }.apply {
                    val items = (1..400).map { it.toString() }
                    adapter = CustomAdapter(items)
                    numColumns = 6
                }.also { list = it },
                MATCH_PARENT, MATCH_PARENT
            )

            layout = this
        }
        return layout
    }

    class CustomAdapter(
        private val items: List<String>,
    ) : BaseAdapter() {

        private var viewCreationId = 0

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val textView = if (convertView is TextView) {
                convertView
            } else {
                val viewId = ++viewCreationId

                val newView = object : TextView(parent.context) {
                    var tmpDetach = false

                    override fun onAttachedToWindow() {
                        super.onAttachedToWindow()
                        Log.i(null, "View $viewId attached")
                    }

                    override fun onDetachedFromWindow() {
                        super.onDetachedFromWindow()
                            Log.i(null, "View $viewId detached")
                            tmpDetach = false

                    }

                    override fun onStartTemporaryDetach() {
                        if (tmpDetach) {
                            throw AssertionError()
                        }
                        super.onStartTemporaryDetach()
                        Log.i(null, "View $viewId start temp detach")
                        tmpDetach = true
                    }

                    override fun onFinishTemporaryDetach() {
                        if (!tmpDetach) {
                            throw AssertionError()
                        }
                        super.onStartTemporaryDetach()
                        Log.i(null, "View $viewId finish temp detach")
                        tmpDetach = false
                    }

                    protected fun finalize() {
                        Log.i(
                            null, "View $viewId finalized, attached: $isAttachedToWindow, parent: $parent"
                        )
                        if (isAttachedToWindow) {
                            throw AssertionError()
                        }
                    }
                }
                newView.tag = viewId
                newView.textSize = ((Math.random() * 50).toInt() + 50).toFloat()
                newView.background = Color.argb(128, 20, (Math.random() * 100).toInt() + 100, (Math.random() * 150).toInt() + 100).toDrawable()
                newView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, newView.dp(if (position % 12 < 6) 240F else 128F))
                Log.i(null, "View $viewId created")
                newView
            }
            textView.text = items[position]
            return textView
        }

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun isEnabled(position: Int): Boolean {
            return false
        }
    }
}