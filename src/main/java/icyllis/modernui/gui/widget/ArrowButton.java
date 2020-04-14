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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;

public class ArrowButton extends AnimatedWidget {

    private final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    private final float u;

    private float tx, ty;

    private boolean available;

    private float brightness = 0.8f;

    private Runnable leftClickFunc;

    public ArrowButton(Direction direction, Runnable leftClick, boolean available) {
        super(12, 12);
        u = 32 * direction.ordinal();
        this.leftClickFunc = leftClick;
        this.available = available;
        if (!available) {
            brightness = 0.3f;
        }
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        RenderSystem.pushMatrix();
        RenderSystem.scalef(0.375f, 0.375f, 1);
        RenderSystem.color3f(brightness, brightness, brightness);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(tx, ty, u, 64, 32, 32);
        RenderSystem.popMatrix();
    }

    @Override
    public void setPos(float x, float y) {
        super.setPos(x, y);
        tx = x * 2.6666666f;
        ty = y * 2.6666666f;
    }

    public void setAvailable(boolean available) {
        this.available = available;
        if (available) {
            if (mouseHovered) {
                manager.addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 1.0f, v -> brightness = v))
                        .onFinish(() -> setOpenState(true)));
            } else {
                manager.addAnimation(new Animation(2)
                        .applyTo(new Applier(brightness, 0.8f, v -> brightness = v)));
            }
        } else {
            manager.addAnimation(new Animation(2)
                    .applyTo(new Applier(brightness, 0.3f, v -> brightness = v)));
        }
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0) {
            if (available) {
                brightness = 0.85f;
                leftClickFunc.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (listening && available && mouseButton == 0) {
            brightness = 1.0f;
            return true;
        }
        return false;
    }

    @Override
    protected void createOpenAnimations() {
        if (available) {
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(0.8f, 1.0f, v -> brightness = v))
                    .onFinish(() -> setOpenState(true)));
        } else {
            setOpenState(true);
        }
    }

    @Override
    protected void createCloseAnimations() {
        if (available) {
            manager.addAnimation(new Animation(3)
                    .applyTo(new Applier(1.0f, 0.8f, v -> brightness = v))
                    .onFinish(() -> setOpenState(false)));
        } else {
            setOpenState(false);
        }
    }

    public enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }
}
