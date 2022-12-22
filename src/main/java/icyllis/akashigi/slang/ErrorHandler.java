/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.ir.Node;

/**
 * Class which is notified in the event of an error.
 */
public abstract class ErrorHandler {

    private String mSource;
    private int mErrorCount;
    private int mWarningCount;

    public ErrorHandler() {
    }

    public final String getSource() {
        return mSource;
    }

    public final void setSource(String source) {
        mSource = source;
    }

    public final int getErrorCount() {
        return mErrorCount;
    }

    public final int getWarningCount() {
        return mWarningCount;
    }

    public final void resetCount() {
        mErrorCount = 0;
        mWarningCount = 0;
    }

    public final void error(int position, String msg) {
        error(Node.getStartOffset(position), Node.getEndOffset(position), msg);
    }

    public final void error(int start, int end, String msg) {
        if (msg.contains(Compiler.POISON_TAG)) {
            // Don't report errors on poison values.
            return;
        }
        mErrorCount++;
        handleError(start, end, msg);
    }

    public final void warning(int position, String msg) {
        warning(Node.getStartOffset(position), Node.getEndOffset(position), msg);
    }

    public final void warning(int start, int end, String msg) {
        mWarningCount++;
        handleWarning(start, end, msg);
    }

    /**
     * Called when an error is reported.
     */
    protected abstract void handleError(int start, int end, String msg);

    /**
     * Called when a warning is reported.
     */
    protected abstract void handleWarning(int start, int end, String msg);
}
