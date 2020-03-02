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

import icyllis.modernui.api.template.IButtonT1;
import icyllis.modernui.api.builder.IEventListenerInitializer;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.gui.element.IBase;
import icyllis.modernui.gui.element.TextLine;
import icyllis.modernui.gui.element.Texture2D;
import icyllis.modernui.gui.element.Widget;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.gui.master.GlobalElementBuilder;
import icyllis.modernui.system.ModernUI;

import java.util.function.Consumer;

public class ButtonT1 extends Widget<Object> implements IButtonT1 {

    protected Texture2D texture;

    private TextLine hoverText = TextLine.DEFAULT;

    public ButtonT1() {
        setListener(this);
    }

    @Override
    public void draw() {
        super.draw();
        texture.draw();
        //hoverText.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        texture.resize(width, height);
        //hoverText.resize(width, height);
    }

    @Override
    public IButtonT1 createHoverTextLine(Consumer<ITextLineBuilder> builderConsumer) {
        TextLine t = new TextLine();
        builderConsumer.accept(t);
        hoverText = t;
        return this;
    }

    @Override
    public IButtonT1 createTexture(Consumer<ITextureBuilder> builderConsumer) {
        texture = GlobalElementBuilder.INSTANCE.texture().buildForMe();
        builderConsumer.accept(texture);
        GlobalAnimationManager.INSTANCE
                .create(a -> a
                        .setInit(0)
                        .setTarget(1.0f)
                        .setTiming(3.0f)
                        .setDelay(1.0f),
                        r -> texture.alpha = r);
        Consumer<Boolean> c = GlobalAnimationManager.INSTANCE
                .createHS(a -> a
                        .setInit(0.5f)
                        .setTarget(1.0f)
                        .setTiming(4.0f),
                        r -> texture.tintR = texture.tintG = texture.tintB = r);
        listener.addHoverOn(s -> c.accept(true));
        listener.addHoverOff(s -> c.accept(false));
        return this;
    }

    @Override
    public IButtonT1 initEventListener(Consumer<IEventListenerInitializer> builderConsumer) {
        builderConsumer.accept(listener);
        return this;
    }

    @Override
    public IButtonT1 onLeftClick(Runnable runnable) {
        listener.addLeftClick(s -> runnable.run());
        return this;
    }

    @Override
    public void buildToPool(Consumer<IBase> pool) {
        pool.accept(this);
    }
}
