/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler;

/**
 * Class which is notified in the event of an error.
 */
public abstract class ErrorHandler {

    protected String mSource;
    private int mErrors;
    private int mWarnings;

    public final String getSource() {
        return mSource;
    }

    public final void setSource(String source) {
        mSource = source;
    }

    public final int errorCount() {
        return mErrors;
    }

    public final int warningCount() {
        return mWarnings;
    }

    public final void reset() {
        mErrors = 0;
        mWarnings = 0;
    }

    /**
     * Reports an error.
     *
     * @param pos see {@link Position}
     * @param msg the error message to report
     */
    public final void error(int pos, String msg) {
        error(Position.getStartOffset(pos), Position.getEndOffset(pos), msg);
    }

    /**
     * Reports an error.
     *
     * @param start the start offset in the source string, or -1
     * @param end   the end offset in the source string, or -1
     * @param msg   the error message to report
     */
    public final void error(int start, int end, String msg) {
        assert (start == -1 && end == -1)
                || (start >= 0 && start <= end && end <= Position.MAX_OFFSET);
        if (msg.contains(ShaderCompiler.POISON_TAG)) {
            // Don't report errors on poison values.
            return;
        }
        mErrors++;
        handleError(start, end, msg);
    }

    /**
     * Reports a warning.
     *
     * @param pos see {@link Position}
     * @param msg the warning message to report
     */
    public final void warning(int pos, String msg) {
        warning(Position.getStartOffset(pos), Position.getEndOffset(pos), msg);
    }

    /**
     * Reports a warning.
     *
     * @param start the start offset in the source string, or -1
     * @param end   the end offset in the source string, or -1
     * @param msg   the warning message to report
     */
    public final void warning(int start, int end, String msg) {
        assert (start == -1 && end == -1)
                || (start >= 0 && start <= end && end <= Position.MAX_OFFSET);
        if (msg.contains(ShaderCompiler.POISON_TAG)) {
            // Don't report errors on poison values.
            return;
        }
        mWarnings++;
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
