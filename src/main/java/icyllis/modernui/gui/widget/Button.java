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

import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IHost;
import icyllis.modernui.gui.master.Widget;
import icyllis.modernui.gui.master.WidgetStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Button extends Widget {

    @Nullable
    private Runnable callback;

    private float brightness = 0.7f;
    private float brightnessOffset = 0;

    /*protected final AnimationControl brightAC = new AnimationControl(Lists.newArrayList(new Animation(4)
            .applyTo(new Applier(0.7f, 1.0f, () -> brightness, this::setBrightness))),
            Lists.newArrayList(new Animation(4)
                    .applyTo(new Applier(0.7f, 0.7f, () -> brightness, this::setBrightness))));*/

    protected boolean locked = false;

    protected final Animation activeAnimation;
    private final Animation inactiveAnimation;

    public Button(IHost host, @Nonnull Builder builder) {
        super(host, builder);
        activeAnimation = new Animation(200)
                .applyTo(new Applier(0.7f, 1.0f, this::getBrightness, this::setBrightness));
        inactiveAnimation = new Animation(100)
                .applyTo(new Applier(0.3f, 0.7f, this::getBrightness, this::setBrightness));
    }

    /**
     * Callback constructor
     * @param r left click callback
     * @return instance
     */
    public Button buildCallback(@Nullable Runnable r) {
        /*if (!b) {
            boolean p = playInactiveAnimation;
            playInactiveAnimation = false;
            setStatus(WidgetStatus.INACTIVE, true);
            playInactiveAnimation = p;
        }*/
        /*if (onetime) {
            if (r != null) {
                callback = () -> {
                    r.run();
                    setStatus(WidgetStatus.INACTIVE, true);
                };
            } else {
                callback = () -> setStatus(WidgetStatus.INACTIVE, true);
            }
            playInactiveAnimation = false;
        } else {*/
        callback = r;
            /*//playInactiveAnimation = true;
        }*/
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas, float time) {
        //brightAC.update();
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (!locked) {
            brightnessOffset = -0.15f;
            if (callback != null) {
                callback.run();
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseLeftRelease(double mouseX, double mouseY) {
        brightnessOffset = 0;
        return true;
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        if (!locked) {
            activeAnimation.start();
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (!locked && getStatus().isListening()) {
            activeAnimation.invert();
        }
    }

    @Override
    protected void onStatusChanged(WidgetStatus status, boolean allowAnimation) {
        super.onStatusChanged(status, allowAnimation);
        activeAnimation.cancel();
        if (status.isListening()) {
            inactiveAnimation.start();
        } else {
            if (allowAnimation) {
                inactiveAnimation.invert();
            } else {
                inactiveAnimation.skipToStart();
            }
        }
        brightnessOffset = 0;
    }

    private void setBrightness(float f) {
        brightness = f;
    }

    private float getBrightness() {
        return brightness;
    }

    protected float getModulatedBrightness() {
        return brightness + brightnessOffset;
    }
}
