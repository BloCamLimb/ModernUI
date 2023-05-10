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

package icyllis.modernui.test;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.TextView;
import icyllis.muix.viewpager.widget.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import static icyllis.modernui.view.View.dp;

public class TestViewPager extends Fragment {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.ALL);
        try (ModernUI app = new ModernUI()) {
            app.run(new TestViewPager());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        var pager = new ViewPager();

        pager.setAdapter(new MyAdapter());
        pager.setFocusableInTouchMode(true);
        pager.setKeyboardNavigationCluster(true);

        // press 'tab' key to take focus, and use arrow keys
        pager.setOnFocusChangeListener((v, hasFocus) ->
                ModernUI.LOGGER.info("{} focus change: {}", v, hasFocus));

        {
            var indicator = new LinearPagerIndicator();
            indicator.setPager(pager);
            var lp = new ViewPager.LayoutParams();
            lp.height = dp(30);
            lp.isDecor = true;
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            pager.addView(indicator, lp);
        }

        var lp = new FrameLayout.LayoutParams(dp(480), dp(360));
        lp.gravity = Gravity.CENTER;
        pager.setLayoutParams(lp);

        // used when pages change too fast
        //pager.setOffscreenPageLimit(2);

        return pager;
    }

    static class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 7;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            var tv = new TextView();
            tv.setText("This is page " + position);
            tv.setGravity(Gravity.CENTER);
            container.addView(tv);
            return tv;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}
