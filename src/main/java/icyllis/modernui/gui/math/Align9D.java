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

package icyllis.modernui.gui.math;

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

    private Align3H align3H;

    private Align3V align3V;

    Align9D() {
        this.align3H = Align3H.getFrom9D(this);
        this.align3V = Align3V.getFrom9D(this);
    }

    public Align3H getAlign3H() {
        return align3H;
    }

    public Align3V getAlign3V() {
        return align3V;
    }

    public static Align9D combineWith(Align3H align3H, Align3V align3V) {
        return values()[align3H.ordinal() + align3V.ordinal() * 3];
    }
}
