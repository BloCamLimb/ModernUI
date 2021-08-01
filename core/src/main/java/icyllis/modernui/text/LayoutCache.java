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

import javax.annotation.Nonnull;

public class LayoutCache {



    private static class Key {

        private CharSequence ref;
        private int hash;

        public Key() {
        }

        /**
         * Copy constructor
         */
        private Key(@Nonnull CharSequence from, int hash) {
            this.ref = from.toString();
            this.hash = hash;
        }

        public void update(CharSequence text) {
            ref = text;
            hash = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CharSequence other = ((Key) o).ref;

            final int len = ref.length();
            if (len != other.length()) {
                return false;
            }

            final CharSequence s = ref;
            for (int i = 0; i < len; i++) {
                if (s.charAt(i) != other.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                final CharSequence s = ref;
                for (int i = 0, e = s.length(); i < e; i++) {
                    h = h * 31 + s.charAt(i);
                }
                return hash = h;
            }
            return h;
        }

        @Nonnull
        public Key copy() {
            return new Key(ref, hash);
        }
    }
}
