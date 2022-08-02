/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.sksl;

/**
 * Class which is notified in the event of an error.
 */
public abstract class ErrorReporter {

    private String mSource;
    private int mErrorCount;

    public ErrorReporter() {
    }

    public final void error(int start, int end, String msg) {
        if (msg.contains(Compiler.POISON_TAG)) {
            // Don't report errors on poison values.
            return;
        }
        mErrorCount++;
        handleError(start, end, msg);
    }

    /**
     * Called when an error is reported.
     */
    protected abstract void handleError(int start, int end, String msg);

    public final String getSource() {
        return mSource;
    }

    public final void setSource(String source) {
        mSource = source;
    }

    public final int getErrorCount() {
        return mErrorCount;
    }

    public final void resetErrorCount() {
        mErrorCount = 0;
    }
}
