/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.master;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A drag and drop operation shares the same DragEvent instance
 *
 * @since 2.0
 */
public final class DragEvent {

    public static final int ACTION_DRAG_STARTED = 1;

    public static final int ACTION_DRAG_ENTERED = 2;

    public static final int ACTION_DRAG_UPDATED = 3;

    public static final int ACTION_DRAG_EXITED = 4;

    public static final int ACTION_DROP = 5;

    public static final int ACTION_DRAG_ENDED = 6;

    private int action;

    private double x;
    private double y;

    private final DragData data;

    private boolean result;

    /**
     * A drag and drop operation only creates an instance by system
     *
     * @param data drag data
     */
    DragEvent(DragData data) {
        this.data = data;
    }

    public int getAction() {
        return action;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Object getData() {
        return data;
    }

    public boolean getResult() {
        return result;
    }

    // Internal method
    void setAction(int action) {
        this.action = action;
    }

    // Internal method
    void setPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Internal method
    void setResult(boolean result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("action", action)
                .append("x", x)
                .append("y", y)
                .append("data", data)
                .append("result", result)
                .toString();
    }
}
