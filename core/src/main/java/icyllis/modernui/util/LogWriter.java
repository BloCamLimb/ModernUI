/*
 * Modern UI.
 * Copyright (C) 2019-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;

import java.io.Writer;

/**
 * Buffered writer to logger, auto flush on newline.
 *
 * @hidden
 */
@ApiStatus.Internal
public class LogWriter extends Writer {

    private static final int FORCE_WRAP_LENGTH = 960;

    private final int mLevel;
    private final Marker mMarker;
    private final StringBuilder mBuilder = new StringBuilder(120);

    public LogWriter(@Nullable Marker marker) {
        this(Log.DEBUG, marker);
    }

    /**
     * Create a new Writer that sends to the log with the given tag.
     */
    public LogWriter(@Log.Level int level,
                     @Nullable Marker marker) {
        mLevel = level;
        mMarker = marker;
    }

    @Override
    public void close() {
        flushBuilder();
    }

    @Override
    public void flush() {
        flushBuilder();
    }

    @Override
    public void write(@NonNull char[] buf, int offset, int count) {
        mBuilder.ensureCapacity(
                Math.min(mBuilder.length() + count,
                        FORCE_WRAP_LENGTH + 2)
        );
        for (int i = 0; i < count; i++) {
            char c = buf[offset + i];
            if (c == '\n') {
                flushBuilder();
            } else {
                mBuilder.append(c);
                if (mBuilder.length() >= FORCE_WRAP_LENGTH) {
                    flushBuilder();
                }
            }
        }
    }

    private void flushBuilder() {
        if (mBuilder.length() != 0) {
            Log.println(mLevel, mMarker, mBuilder.toString());
            mBuilder.setLength(0);
        }
    }
}
