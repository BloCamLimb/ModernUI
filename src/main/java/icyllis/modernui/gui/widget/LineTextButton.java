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

import com.google.common.collect.Lists;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.module.PopupContextMenu;

import java.util.function.Function;
import java.util.function.Predicate;

public class LineTextButton extends StateAnimatedButton {

    protected String text;

    protected final float width, halfWidth;

    protected float widthOffset;

    protected float textBrightness = 0.7f;

    protected float alpha = 0;

    protected boolean lock = false;

    public LineTextButton(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, float width) {
        super(xResizer, yResizer);
        this.text = text;
        this.width = width;
        this.widthOffset = this.halfWidth = width / 2f;
        this.shape = new FixedShape.Rect(width, 12);
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(1f, value -> alpha = value))
                .withDelay(1));
    }

    @Override
    public void draw(float currentTime) {
        super.checkState();
        fontRenderer.drawString(text, x + halfWidth, y + 2, textBrightness, alpha, TextAlign.CENTER);
        DrawTools.fillRectWithColor(x + widthOffset, y + 11, x + width - widthOffset, y + 12, 0xffffff, alpha);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    protected void onOpen() {
        super.onOpen();
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(halfWidth, 0, value -> widthOffset = value),
                        new Applier(textBrightness, 1, value -> textBrightness = value))
                .onFinish(() -> openState = 2));
    }

    public void onModuleChanged(int id) {

    }

    @Override
    protected void onMouseHoverOn() {
        if (!lock)
            moduleManager.addAnimation(new Animation(3)
                    .applyTo(new Applier(0.7f, 1, value -> textBrightness = value)));
    }

    @Override
    protected void onMouseHoverOff() {
        if (!lock)
            moduleManager.addAnimation(new Animation(3)
                    .applyTo(new Applier(1, 0.7f, value -> textBrightness = value)));
    }

    @Override
    protected void onClose() {
        super.onClose();
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(0, halfWidth, value -> widthOffset = value),
                        new Applier(textBrightness, 0.7f, value -> textBrightness = value))
                .onFinish(() -> openState = 0));
    }

    @Override
    public void startClose() {
        if (!lock)
            super.startClose();
    }

    public static class A extends LineTextButton {

        private int moduleID;

        public A(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, float width, int moduleID) {
            super(xResizer, yResizer, text, width);
            this.moduleID = moduleID;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            if (mouseHovered && !lock && mouseButton == 0) {
                GlobalModuleManager.INSTANCE.switchModule(moduleID);
                return true;
            }
            return false;
        }

        @Override
        public void onModuleChanged(int id) {
            if (id == moduleID) {
                lock = true;
                startOpen();
            } else {
                lock = false;
                startClose();
            }
        }
    }

    public static class B extends LineTextButton {

        private Predicate<Integer> module;

        private int id;

        public B(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, String text, float width, Predicate<Integer> module) {
            super(xResizer, yResizer, text, width);
            this.module = module;
        }

        @Override
        public void draw(float currentTime) {
            super.draw(currentTime);
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            super.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            if (mouseHovered && mouseButton == 0) {
                DropDownMenu list = new DropDownMenu(Lists.newArrayList("Resource Packs", "Shaders", "Fonts", "Language"), id - 35, 12, this::menuActions);
                list.setPos(x + width, y + 13, GlobalModuleManager.INSTANCE.getWindowHeight());
                GlobalModuleManager.INSTANCE.openPopup(new PopupContextMenu(list), false);
                return true;
            }
            return false;
        }

        private void menuActions(int index) {
            if (index >= 0 && index <= 4)
                GlobalModuleManager.INSTANCE.switchModule(index + 35);
        }

        @Override
        public void onModuleChanged(int id) {
            this.id = id;
            if (module.test(id)) {
                lock = true;
                startOpen();
            } else {
                lock = false;
                startClose();
            }
        }
    }
}
