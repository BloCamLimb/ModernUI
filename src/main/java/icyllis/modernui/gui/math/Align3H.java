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

import javax.annotation.Nonnull;

public enum Align3H {
    LEFT(0.0f),
    CENTER(0.25f),
    RIGHT(0.5f);

    private float textOffset;

    Align3H(float textOffset) {
        this.textOffset = textOffset;
    }

    public float getTextOffset() {
        return textOffset;
    }

    public static Align3H getFrom9D(@Nonnull Align9D align9D) {
        return values()[align9D.ordinal() % 3];
    }
}
