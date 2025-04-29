/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.sketch.Surface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//TODO wip
public final class RenderNode extends RenderProperties {

    private Canvas mCurrentRecordingCanvas;
    @SharedPtr
    public Surface mLayerSurface;

    /**
     * Creates a new RenderNode that can be used to record batches of
     * drawing operations, and store / apply render properties when drawn.
     */
    public RenderNode() {
    }

    /**
     * Starts recording a display list for the render node. All
     * operations performed on the returned canvas are recorded and
     * stored in this display list.
     * <p>
     * {@link #endRecording()} must be called when the recording is finished in order to apply
     * the updated display list. Failing to call {@link #endRecording()} will result in an
     * {@link IllegalStateException} if this method is called again.
     *
     * @param width  The width of the recording viewport. This will not alter the width of the
     *               RenderNode itself, that must be set with {@link #setPosition(Rect)}.
     * @param height The height of the recording viewport. This will not alter the height of the
     *               RenderNode itself, that must be set with {@link #setPosition(Rect)}.
     * @return A canvas to record drawing operations.
     * @throws IllegalStateException If a recording is already in progress. That is, the previous
     *                               call to this method did not call {@link #endRecording()} first.
     * @see #endRecording()
     * @see #hasDisplayList()
     */
    @Nonnull
    public Canvas beginRecording(int width, int height) {
        if (mCurrentRecordingCanvas != null) {
            throw new IllegalStateException("Recording currently in progress - missing #endRecording() call?");
        }
        //mCurrentRecordingCanvas = RecordingCanvas.obtain(this, width, height);
        return mCurrentRecordingCanvas;
    }

    /**
     * Ends the recording for this display list. Calling this method marks
     * the display list valid and {@link #hasDisplayList()} will return true.
     *
     * @see #beginRecording(int, int)
     * @see #hasDisplayList()
     */
    public void endRecording() {
        if (mCurrentRecordingCanvas == null) {
            throw new IllegalStateException("No recording in progress, forgot to call #beginRecording()?");
        }
        Canvas canvas = mCurrentRecordingCanvas;
        mCurrentRecordingCanvas = null;
        //canvas.finishRecording(this);
        //canvas.recycle();
    }

    public void setOutline(@Nullable Outline outline) {
        if (outline == null) {
            getOutline().setNone();
            return;
        }
        switch (outline.getType()) {
            case Outline.TYPE_EMPTY -> getOutline().setEmpty();
            case Outline.TYPE_ROUND_RECT -> {
                var bounds = outline.getBounds();
                var radius = outline.getRadius();
                getOutline().setRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, radius);
                getOutline().setAlpha(outline.getAlpha());
            }
            default -> throw new IllegalStateException();
        }
    }
}
