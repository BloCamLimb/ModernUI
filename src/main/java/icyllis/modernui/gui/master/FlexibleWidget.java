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

package icyllis.modernui.gui.master;

import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nonnull;
import java.util.function.Function;

public abstract class FlexibleWidget extends Widget {

    private final Function<Integer, Float> xResizer, yResizer;

    private final Function<Integer, Float> wResizer, hResizer;

    public FlexibleWidget(Module module, Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        super(module);
        this.xResizer = xResizer;
        this.yResizer = yResizer;
        this.wResizer = wResizer;
        this.hResizer = hResizer;
    }

    @Deprecated
    @Override
    public final void locate(float px, float py) {
        throw new RuntimeException();
    }

    @Deprecated
    @Override
    public final void setLocator(@Nonnull Locator locator) {
        throw new RuntimeException();
    }

    @Override
    public void resize(int width, int height) {
        this.x1 = xResizer.apply(width);
        this.y1 = yResizer.apply(height);
        this.width = wResizer.apply(width);
        this.height = hResizer.apply(height);
        this.x2 = x1 + this.width;
        this.y2 = y1 + this.height;
    }
}
