/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.layout;

// NatsuiroMatsuri: Ore ga hololive da!
// yagoo!
public enum Align9D {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    private final int hIndex;

    private final int vIndex;

    Align9D() {
        hIndex = ordinal() % 3;
        vIndex = ordinal() / 3;
    }

    public float getAlignedX(float px, float width) {
        switch (hIndex) {
            case 0:
                return px;
            case 1:
                return px + width / 2.0f;
            case 2:
                return px + width;
        }
        return px;
    }

    public float getAlignedY(float py, float height) {
        switch (vIndex) {
            case 0:
                return py;
            case 1:
                return py + height / 2.0f;
            case 2:
                return py + height;
        }
        return py;
    }

    public boolean isLeft() {
        return hIndex == 0;
    }

    public boolean isHCenter() {
        return hIndex == 1;
    }

    public boolean isRight() {
        return hIndex == 2;
    }

    public boolean isTop() {
        return vIndex == 0;
    }

    public boolean isVCenter() {
        return vIndex == 1;
    }

    public boolean isBottom() {
        return vIndex == 2;
    }
}
