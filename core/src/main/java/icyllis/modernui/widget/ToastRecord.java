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

package icyllis.modernui.widget;

public final class ToastRecord {

    public final Toast mToken;
    public final CharSequence mText;

    private int mDuration;

    ToastRecord(Toast token, CharSequence text, int duration) {
        mToken = token;
        mText = text;
        mDuration = duration;
    }

    /**
     * Returns the duration of this toast, which can be {@link Toast#LENGTH_SHORT}
     * or {@link Toast#LENGTH_LONG}.
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Updates toast duration.
     */
    public void update(int duration) {
        mDuration = duration;
    }
}
