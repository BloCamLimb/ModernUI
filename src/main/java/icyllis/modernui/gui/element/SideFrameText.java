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

package icyllis.modernui.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.api.element.IElement;
import icyllis.modernui.gui.animation.DisposableSinAnimation;
import icyllis.modernui.gui.animation.DisposableUniAnimation;
import icyllis.modernui.gui.font.IFontRenderer;
import icyllis.modernui.gui.font.StringRenderer;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;

public class SideFrameText implements IElement {

    private IFontRenderer renderer = StringRenderer.STRING_RENDERER;

    private String text;

    private float x, y;

    private float frameOpacity = 0, textOpacity = 0;

    private float sizeW = 0;

    // 0=close 1=opening 2=open 3=closing
    private int openState = 0;

    private boolean prepareToClose = false;

    private boolean prepareToOpen = false;

    public SideFrameText(String text) {
        this.text = text;
    }

    @Override
    public void draw(float currentTime) {
        if (prepareToOpen && openState == 0) {
            open();
            prepareToOpen = false;
        } else if (prepareToClose && openState == 2) {
            close();
            prepareToClose = false;
        }
        if (openState == 0) {
            return;
        }
        DrawTools.fillRectWithFrame(x - 4, y - 3, x + sizeW, y + 11, 1, 0, 0, 0, 0.4f * frameOpacity, 0.25f, 0.25f, 0.25f, 0.8f * frameOpacity);
        RenderSystem.enableBlend();
        renderer.drawString(text, x, y, 1, 1, 1, textOpacity, 0);
    }

    private void open() {
        openState = 1;
        float textLength = renderer.getStringWidth(text);
        GlobalModuleManager.INSTANCE.addAnimation(new DisposableSinAnimation(-4, textLength + 4, 3, value -> sizeW = value));
        GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(0, 1, 3, value -> frameOpacity = value));
        GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(0, 1, 3, value -> textOpacity = value).withDelay(2).onFinish(() -> openState = 2));
    }

    private void close() {
        openState = 3;
        GlobalModuleManager.INSTANCE.addAnimation(new DisposableUniAnimation(1, 0, 5, value -> textOpacity = frameOpacity = value).onFinish(() -> openState = 0));
    }

    public void startOpen() {
        if (openState == 0 || openState == 3) {
            prepareToOpen = true;
        }
    }

    public void startClose() {
        if (openState == 1 || openState == 2) {
            prepareToClose = true;
        }
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void resize(int width, int height) {

    }
}
