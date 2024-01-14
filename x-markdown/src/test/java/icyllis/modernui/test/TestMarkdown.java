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
import icyllis.modernui.markdown.Markdown;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.Log;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.TextView;

public class TestMarkdown extends Fragment {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Log.setLevel(Log.DEBUG);
        try (ModernUI app = new ModernUI()) {
            app.run(new TestMarkdown());
        }
        System.gc();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setTextSize(14);
        tv.setTextIsSelectable(true);
        Markdown.create(requireContext())
                .setMarkdown(tv, """
                        Advanced Page
                        ---
                        My **First** Line
                        > My *Second* Line
                        * One
                          * ```java
                            public static void main(String[] args) {
                            }
                            ```
                          * Three
                            * Four
                                                
                        [Modern UI](https://github.com/BloCamLimb/ModernUI)
                        1. One
                        2. Two
                        3. Three
                        # Heading 1
                        ## Heading 2
                        ### Heading 3
                                                
                        AAA AAA
                        ******
                        BBB BBB
                        """);
        tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        return tv;
    }
}
