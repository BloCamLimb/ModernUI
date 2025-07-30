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
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.BaseExpandableListAdapter
import icyllis.modernui.widget.ExpandableListView
import icyllis.modernui.widget.TextView
import icyllis.modernui.widget.Toast

fun main() {
    System.setProperty("java.awt.headless", "true")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    ModernUI().use { app -> app.run(TestExpandableListView()) }
}

class TestExpandableListView : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: DataSet?
    ): View {
        val listView = ExpandableListView(context)
        listView.setAdapter(TheExpandableListAdapter())
        listView.setOnChildClickListener { parent, v, _, _, _ ->
            Toast.makeText(
                parent.context,
                (v as TextView).text, Toast.LENGTH_SHORT
            ).show()
            true
        }
        val dp20 = listView.dp(20f)
        listView.setPadding(dp20, dp20, dp20, dp20)
        return listView
    }

    internal class TheExpandableListAdapter : BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return GROUPS.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return ITEMS_ARRAY[groupPosition].size
        }

        override fun getGroup(groupPosition: Int): Any {
            return GROUPS[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return ITEMS_ARRAY[groupPosition][childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val tv: TextView = if (convertView == null) {
                TextView(parent.context)
            } else {
                convertView as TextView
            }
            tv.text = GROUPS[groupPosition]
            tv.textSize = 16f
            tv.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            val dp4 = tv.dp(4f)
            tv.setPadding(dp4 * 10, dp4, dp4, dp4)
            return tv
        }

        override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val tv: TextView = if (convertView == null) {
                TextView(parent.context)
            } else {
                convertView as TextView
            }
            tv.text = ITEMS_ARRAY[groupPosition][childPosition]
            tv.textSize = 14f
            tv.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            val dp4 = tv.dp(4f)
            tv.setPadding(dp4 * 10, dp4, dp4, dp4)
            return tv
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

        companion object {
            private val GROUPS = arrayOf(
                "Players",
                "Waypoints",
                "Metadata"
            )
            private val ITEMS_ARRAY = arrayOf(
                arrayOf(
                    "BloCamLimb",
                    "mosquitozzi",
                    "zzzz_ustc",
                    "Linngdu664",
                    "idyllic_bean",
                    "Seraph_JACK"
                ),
                arrayOf(
                    "Base",
                    "Miner",
                    "Generator",
                    "Farm",
                    "Storage"
                ),
                arrayOf(
                    "Meta A",
                    "Meta B",
                    "Meta C",
                    "Meta D"
                )
            )
        }
    }
}
