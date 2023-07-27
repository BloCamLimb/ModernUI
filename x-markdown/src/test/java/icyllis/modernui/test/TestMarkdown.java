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
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.markdown.Markdown;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.TextView;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import javax.annotation.Nonnull;

public class TestMarkdown extends Fragment {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.DEBUG);

        try (ModernUI app = new ModernUI()) {
            app.run(new TestMarkdown());
        }
        AudioManager.getInstance().close();
        System.gc();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        TextView tv = new TextView(getContext());
        tv.setTextSize(16);
        tv.setText(Markdown.create()
                .convert("""
                        Advanced Page
                        ---
                        My **First** Line
                        > My *Second* Line
                        * One
                          * Two
                          * Three
                            * Four
                        * Five
                        >>- one
                        >>
                          >  > two
                        
                        public static void 哈哈()
                        """));
        {
            var lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            tv.setLayoutParams(lp);
        }
        return tv;
    }

    @Nonnull
    private static String toEscapeChars(@Nonnull CharSequence a) {
        int iMax = a.length() - 1;
        if (iMax == -1)
            return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append("\\u");
            String s = Integer.toHexString(a.charAt(i));
            b.append("0".repeat(4 - s.length()));
            b.append(s);
            if (i == iMax)
                return b.toString();
        }
    }
}
