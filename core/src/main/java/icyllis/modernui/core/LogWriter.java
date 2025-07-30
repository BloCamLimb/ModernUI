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

package icyllis.modernui.core;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.io.Writer;

/**
 * Buffered writer to logger, auto flush on newline.
 */
public class LogWriter extends Writer {

    private static final int FORCE_WRAP_LENGTH = 960;

    private final Logger mLogger;
    private final Level mLevel;
    private final Marker mMarker;
    private final StringBuilder mBuilder = new StringBuilder(120);

    public LogWriter(@NonNull Logger logger) {
        this(logger, Level.DEBUG, null);
    }

    /**
     * Create a new Writer that sends to the log with the given tag.
     */
    public LogWriter(@NonNull Logger logger,
                     @NonNull Level level,
                     @Nullable Marker marker) {
        mLogger = logger;
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
            var msg = mBuilder.toString();
            var e = mLogger.atLevel(mLevel);
            if (mMarker != null) {
                e = e.addMarker(mMarker);
            }
            e.setMessage(msg).log();
            mBuilder.setLength(0);
        }
    }
}
