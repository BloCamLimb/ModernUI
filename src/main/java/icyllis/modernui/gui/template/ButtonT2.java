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

package icyllis.modernui.gui.template;

import icyllis.modernui.api.template.IButtonT2;
import icyllis.modernui.gui.element.IBase;
import icyllis.modernui.gui.element.Rectangle;
import icyllis.modernui.gui.element.TextLine;
import icyllis.modernui.gui.element.Widget;
import icyllis.modernui.gui.master.GlobalAnimationManager;

import java.util.function.Consumer;
import java.util.function.Function;

public class ButtonT2 extends Widget<Object> implements IButtonT2 {

    private Rectangle rect;

    private TextLine textline;

    private boolean drawRect = false;

    private boolean selected = false;

    public ButtonT2() {
        setListener(this);
    }

    @Override
    public void draw() {
        super.draw();
        rect.draw();
        textline.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        textline.resize(width, height);
        rect.resize(width, height);
    }

    @Override
    public IButtonT2 init(Function<Integer, Float> x, Function<Integer, Float> y, String text) {
        this.textline = new TextLine();
        textline.init(w -> x.apply(w) + 24, h -> y.apply(h) + 2, () -> text, 0.25f, 0xffcccccc, 1);
        this.rect = new Rectangle();
        rect.init(x, w -> y.apply(w) + 10, w -> 48f, h -> 2f, 0x00ffffff);
        listener.setPos(x, y);
        listener.setRectShape(48, 12);
        Consumer<Boolean> c1 = GlobalAnimationManager.INSTANCE
                .createHS(a -> a
                        .setInit(0)
                        .setTarget(0.8f)
                        .setTiming(4),
                        r -> rect.alpha = r);
        Consumer<Boolean> c2 = GlobalAnimationManager.INSTANCE
                .createHS(a -> a
                        .setInit(0.8f)
                        .setTarget(1.0f)
                        .setTiming(4),
                        r -> textline.colorR = textline.colorG = textline.colorB = r);
        listener.addHoverOn(q -> {
            c1.accept(true);
            c2.accept(true);
            drawRect = true;
        });
        listener.addHoverOff(q -> {
            c1.accept(false);
            c2.accept(false);
            drawRect = false;
        });
        return this;
    }

    @Override
    public void buildToPool(Consumer<IBase> pool) {
        pool.accept(this);
    }
}
