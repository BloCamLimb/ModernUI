/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

import icyllis.modernui.widget.WidgetArea;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Example
 */
@Deprecated
public class StandardEventListener /*implements IGuiEventListener*/ {

    protected Function<Integer, Float> xResizer, yResizer;

    protected float x, y;

    protected WidgetArea shape;

    /** whether this listener is active (false will skip) **/
    protected boolean available = true;

    /** whether mouse hovered on this **/
    protected boolean mouseHovered = false;

    protected List<iEvent> events = new ArrayList<>();

    public StandardEventListener(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, WidgetArea shape) {
        this.xResizer = xResizer;
        this.yResizer = yResizer;
        this.shape = shape;
    }

    public void resize(int width, int height) {
        float x = xResizer.apply(width);
        float y = yResizer.apply(height);
        this.x = x;
        this.y = y;
    }

    public void mouseMoved(double mouseX, double mouseY) {
        if (!available) {
            return;
        }
        boolean previous = mouseHovered;
        this.mouseHovered = this.isMouseOver(mouseX, mouseY);
        if (previous != mouseHovered) {
            if (mouseHovered) {
                events.stream().filter(e -> e.id == iEvent.MOUSE_HOVER_ON).forEach(iEvent::run);
            } else {
                events.stream().filter(e -> e.id == iEvent.MOUSE_HOVER_OFF).forEach(iEvent::run);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!available) {
            return false;
        }
        if (mouseButton == 0 && mouseHovered) {
            events.stream().filter(e -> e.id == iEvent.LEFT_CLICK).forEach(iEvent::run);
            return true;
        }
        return false;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return shape.isMouseInArea(x, y, mouseX, mouseY);
    }

    public void addHoverOn(Runnable runnable) {
        events.add(new iEvent(iEvent.MOUSE_HOVER_ON, runnable));
    }

    public void addHoverOff(Runnable runnable) {
        events.add(new iEvent(iEvent.MOUSE_HOVER_OFF, runnable));
    }

    public void addLeftClick(Runnable runnable) {
        events.add(new iEvent(iEvent.LEFT_CLICK, runnable));
    }

    private static final class iEvent {

        public static int MOUSE_HOVER_ON = 1;
        public static int MOUSE_HOVER_OFF = 2;
        public static int LEFT_CLICK = 3;

        public final int id;

        private final Runnable runnable;

        public iEvent(int id, Runnable runnable) {
            this.id = id;
            this.runnable = runnable;
        }

        public void run() {
            runnable.run();
        }
    }
}
