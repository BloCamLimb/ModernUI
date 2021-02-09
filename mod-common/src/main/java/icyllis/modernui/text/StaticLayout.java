/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text;

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;

public class StaticLayout {

    public final static class Builder {

        private static final Pool<Builder> sPool = Pools.concurrent(3);

        private Builder() {
        }

        public static Builder obtain(@Nonnull CharSequence text, int start, int end) {
            return sPool.acquire();
        }
    }

    void generate() {
        PrecomputedText.ParagraphInfo[] paragraphInfo = null;

        if (paragraphInfo == null) {
            //paragraphInfo = PrecomputedText.createMeasuredParagraphs();
        }

        for (int paraIndex = 0; paraIndex < paragraphInfo.length; paraIndex++) {

        }
    }
}
