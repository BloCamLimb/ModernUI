/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewGroup.LayoutParams;
import icyllis.modernui.widget.BaseAdapter;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.ListView;
import icyllis.modernui.widget.TextView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.modernui.view.View.dp;

public class TestListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        ListView listView = new ListView();
        listView.setAdapter(new MyAdapter(40));
        listView.setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setRGBA(192, 192, 192, 128);
                canvas.drawRect(getBounds(), paint);
            }

            @Override
            public int getIntrinsicHeight() {
                return 2;
            }
        });
        listView.setLayoutParams(new FrameLayout.LayoutParams(dp(400), dp(300), Gravity.CENTER));
        return listView;
    }

    public static class MyAdapter extends BaseAdapter {

        public final int mCount;

        public MyAdapter(int count) {
            mCount = count;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Nonnull
        @Override
        public View getView(int position, @Nullable View convertView, @Nonnull ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = new TextView();
            } else {
                tv = (TextView) convertView;
            }
            tv.setText("Data Entry " + position);
            tv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            tv.setPadding(dp(4), dp(4), dp(4), dp(4));
            return tv;
        }
    }
}
