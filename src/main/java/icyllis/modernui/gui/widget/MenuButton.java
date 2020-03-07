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

package icyllis.modernui.gui.widget;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.element.IElement;
import icyllis.modernui.gui.animation.DisposableUniAnimation;
import icyllis.modernui.gui.animation.HighActiveUniAnimation;
import icyllis.modernui.gui.element.SideFrameText;
import icyllis.modernui.gui.element.StandardTexture;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;
import java.util.function.IntPredicate;

public class MenuButton implements IElement {

    protected EventListener listener;

    protected StandardTexture texture;

    protected HighActiveUniAnimation textureOpacityAnimation;

    public MenuButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, ResourceLocation res, float sizeW, float sizeH, float u, float v, float scale, Runnable onLeftClick) {
        listener = new EventListener(xResizer, yResizer, new Shape.RectShape(sizeW * scale, sizeH * scale));

        texture = new StandardTexture(xResizer, yResizer, sizeW, sizeH, res, u, v, 0x00808080, scale);
        textureOpacityAnimation = new HighActiveUniAnimation(0.5f, 1.0f, 4, value -> texture.tintR = texture.tintG = texture.tintB = value);

        addAnimationEvent();
        listener.addLeftClick(onLeftClick);
        GlobalModuleManager.INSTANCE.addEventListener(listener);
    }

    protected void addAnimationEvent() {
        listener.addHoverOn(() -> textureOpacityAnimation.setStatus(true));
        listener.addHoverOff(() -> textureOpacityAnimation.setStatus(false));
    }

    @Override
    public void draw(float currentTime) {
        textureOpacityAnimation.update(currentTime);
        texture.draw(currentTime);
    }

    @Override
    public void resize(int width, int height) {
        texture.resize(width, height);
        listener.resize(width, height);
    }

    public static class A extends MenuButton {

        private SideFrameText text;

        public A(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, ResourceLocation res, float sizeW, float sizeH, float u, float v, float scale, Runnable onLeftClick) {
            super(xResizer, yResizer, res, sizeW, sizeH, u, v, scale, onLeftClick);
            this.text = new SideFrameText(text);
            GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(0, 1, 3, value -> texture.opacity = value).withDelay(1));
            listener.addHoverOn(() -> this.text.startOpen());
            listener.addHoverOff(() -> this.text.startClose());
        }

        @Override
        public void draw(float currentTime) {
            super.draw(currentTime);
            text.draw(currentTime);
        }

        @Override
        public void resize(int width, int height) {
            super.resize(width, height);
            text.setPos(texture.x + texture.sizeW * texture.scale + 15, texture.y + (texture.sizeH * texture.scale) / 2 - 4);
        }
    }

    public static class B extends A {

        private boolean lock = false;

        public B(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, ResourceLocation res, float sizeW, float sizeH, float u, float v, float scale, Runnable onLeftClick, IntPredicate availability) {
            super(xResizer, yResizer, text, res, sizeW, sizeH, u, v, scale, onLeftClick);
            ModernUI_API.INSTANCE.getModuleManager().addModuleEvent(i -> {
                lock = availability.test(i);
                if(!lock)
                    textureOpacityAnimation.setStatus(false);
            });
        }

        @Override
        protected void addAnimationEvent() {
            listener.addHoverOn(() -> textureOpacityAnimation.setStatus(true));
            listener.addHoverOff(() -> {
                if(!lock)
                    textureOpacityAnimation.setStatus(false);
            });
        }
    }
}
