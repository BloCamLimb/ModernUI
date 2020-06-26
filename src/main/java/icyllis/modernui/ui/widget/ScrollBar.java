/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.master.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class encapsulated methods to handle events and draw the scroll bar.
 * Scroll bar is never a view in UI, but including some view's methods.
 * Scroll bar should be the same level as the view it's in.
 * To control the scroll amount, use {@link Scroller}
 *
 * @since 1.6
 */
@SuppressWarnings("unused")
public class ScrollBar extends View {

    @Nonnull
    private final ICallback callback;

    @Nullable
    private Drawable track;
    @Nullable
    private Drawable thumb;

    private boolean drawTrack;
    private boolean drawThumb;

    private boolean alwaysDrawTrack;

    private boolean thumbHovered;
    private float   thumbOffset;
    private int     thumbLength;

    private float maxScroll;

    private boolean vertical;

    // a vector, not a scalar
    private double accDelta;

    public ScrollBar(@Nonnull ICallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        /*if (!barHovered && !isDragging && brightness > 0.5f) {
            if (canvas.getDrawingTime() > startTime) {
                float change = (startTime - canvas.getDrawingTime()) / 2000.0f;
                brightness = Math.max(0.75f + change, 0.5f);
            }
        }
        canvas.setColor(16, 16, 16, 40);
        canvas.drawRect(getLeft(), getTop(), getRight(), getBottom());
        int br = (int) (brightness * 255.0f);
        canvas.setColor(br, br, br, 128);
        canvas.drawRect(getLeft(), barY, getRight(), barY + barLength);*/

        if (drawTrack && track != null) {
            track.draw(canvas);
        }
        if (drawThumb && thumb != null) {
            // due to gui scaling, we have to do with float rather than integer
            canvas.save();
            if (vertical) {
                canvas.translate(0, thumbOffset);
            } else {
                canvas.translate(thumbOffset, 0);
            }
            thumb.draw(canvas);
            canvas.restore();
        }
    }

    public void setParameters(float range, float offset, float extent, boolean vertical) {
        this.vertical = vertical;
        boolean drawTrack = true;
        boolean drawThumb = true;
        if (extent <= 0 || extent >= range) {
            drawTrack = alwaysDrawTrack;
            drawThumb = false;
        }
        if (drawTrack && track != null) {
            track.setBounds(getLeft(), getTop(), getRight(), getBottom());
        }
        maxScroll = range - extent;
        if (drawThumb && thumb != null) {
            int totalLength = vertical ? getHeight() : getWidth();
            int thickness = vertical ? getWidth() : getHeight();

            float preciseLength = (float) totalLength * extent / range;
            float preciseOffset = (totalLength - preciseLength) * offset / (maxScroll);

            preciseLength = Math.max(preciseLength, thickness << 1);
            thumbLength = Math.round(preciseLength);
            thumbOffset = Math.min(preciseOffset, totalLength - thumbLength);

            if (vertical) {
                thumb.setBounds(getLeft(), getTop(), getRight(), getTop() + thumbLength);
            } else {
                thumb.setBounds(getLeft(), getTop(), getLeft() + thumbLength, getBottom());
            }
        }
        this.drawTrack = drawTrack;
        this.drawThumb = drawThumb;
    }

    public void setTrack(@Nullable Drawable track) {
        this.track = track;
    }

    public void setThumb(@Nullable Drawable thumb) {
        this.thumb = thumb;
    }

    public boolean isAlwaysDrawTrack() {
        return alwaysDrawTrack;
    }

    public void setAlwaysDrawTrack(boolean alwaysDrawTrack) {
        this.alwaysDrawTrack = alwaysDrawTrack;
    }

    /*public void draw(float currentTime) {
        if (!barHovered && !isDragging && brightness > 0.5f) {
            if (currentTime > startTime) {
                float change = (startTime - currentTime) / 40.0f;
                brightness = Math.max(0.75f + change, 0.5f);
            }
        }
        if (!visible) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x + barThickness, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x + barThickness, y, 0.0D).color(16, 16, 16, 40).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(16, 16, 16, 40).endVertex();
        tessellator.draw();

        int b = (int) (brightness * 255);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x + barThickness, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x + barThickness, barY, 0.0D).color(b, b, b, 128).endVertex();
        bufferBuilder.pos(x, barY, 0.0D).color(b, b, b, 128).endVertex();
        tessellator.draw();
    }*/

    /*private void wake() {
        brightness = 0.75f;
        startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
    }*/

    /*public void setBarLength(float percentage) {
        this.barLength = (int) (getHeight() * percentage);
    }

    private float getMaxDragLength() {
        return getHeight() - barLength;
    }

    public void setBarOffset(float percentage) {
        barY = getMaxDragLength() * percentage + getTop();
        wake();
    }*/

    /*@Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (visible) {

            return barHovered;
        }
        return false;
    }*/

    /*@Override
    protected boolean onUpdateMouseHover(int mouseX, int mouseY) {
        boolean prev = barHovered;
        barHovered = isMouseOnBar(mouseY);
        if (prev != barHovered) {
            if (barHovered) {
                wake();
            } else {
                startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
            }
        }
        return super.onUpdateMouseHover(mouseX, mouseY);
    }*/

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        //TODO Drawable States
    }

    @Override
    protected void onMouseHoverUpdate(double mouseX, double mouseY) {
        super.onMouseHoverUpdate(mouseX, mouseY);
        if (vertical) {
            float thumbY = getTop() + thumbOffset;
            thumbHovered = mouseY >= thumbY && mouseY < thumbY + thumbLength;
        } else {
            float thumbX = getLeft() + thumbOffset;
            thumbHovered = mouseX >= thumbX && mouseX < thumbX + thumbLength;
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        thumbHovered = false;
        //TODO Drawable States
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (thumbHovered) {
            UIManager.INSTANCE.setDragging(this);
            return true;
        }
        if (vertical) {
            float thumbY = getTop() + thumbOffset;
            ModernUI.LOGGER.debug("mouseY{} thumbY{} top{}, offset{}", mouseY, thumbY, getTop(), thumbOffset);
            if (mouseY < thumbY) {
                int delta = (int) toScrollDelta((float) (mouseY - thumbY));
                ModernUI.LOGGER.debug("delta{}", delta);
                callback.onClickScrollBar(this, Math.max(-60.0f, delta));
                return true;
            } else if (mouseY > thumbY + thumbLength) {
                int delta = (int) toScrollDelta((float) (mouseY - thumbY - thumbLength));
                callback.onClickScrollBar(this, Math.min(60.0f, delta));
                return true;
            }
        } else {
            float thumbX = getLeft() + thumbOffset;
            if (mouseX < thumbX) {
                int delta = (int) toScrollDelta((float) (mouseX - thumbX));
                callback.onClickScrollBar(this, Math.max(-60.0f, delta));
                return true;
            } else if (mouseX > thumbX + thumbLength) {
                int delta = (int) toScrollDelta((float) (mouseX - thumbX - thumbLength));
                callback.onClickScrollBar(this, Math.min(60.0f, delta));
                return true;
            }
        }
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void onStartDragging() {
        super.onStartDragging();
    }

    @Override
    protected void onStopDragging() {
        super.onStopDragging();
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (vertical) {
            if (mouseY >= getTop() && mouseY <= getBottom()) {
                accDelta += deltaY;
                int i = (int) (accDelta * 2.0f);
                if (i != 0) {
                    float delta = i / 2.0f;
                    accDelta -= delta;
                    delta = toScrollDelta(delta);
                    callback.onDragScrollBar(this, delta);
                    return true;
                }
            }
        } else {
            if (mouseX >= getLeft() && mouseX <= getRight()) {
                accDelta += deltaX;
                int i = (int) (accDelta * 2.0f);
                if (i != 0) {
                    float delta = i / 2.0f;
                    accDelta -= delta;
                    delta = toScrollDelta(delta);
                    callback.onDragScrollBar(this, delta);
                    return true;
                }
            }
        }
        return super.onMouseDragged(mouseX, mouseY, deltaX, deltaY);
    }

    /*@Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        if (barHovered) {
            barHovered = false;
            startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
        }
    }*/

    /*private boolean isMouseOnBar(double mouseY) {
        return mouseY >= barY && mouseY <= barY + barLength;
    }*/

    /*@Override
    protected boolean onMouseLeftClicked(int mouseX, int mouseY) {
        if (barHovered) {
            isDragging = true;
            UIManager.INSTANCE.setDragging(this);
        } else {
            if (mouseY < barY) {
                float mov = transformPosToAmount((float) (barY - mouseY));
                //controller.scrollSmoothBy(-Math.min(60f, mov));
            } else if (mouseY > barY + barLength) {
                float mov = transformPosToAmount((float) (mouseY - barY - barLength));
                //controller.scrollSmoothBy(Math.min(60f, mov));
            }
        }
        return true;
    }*/

    /*@Override
    protected void onStopDragging() {
        super.onStopDragging();
        if (isDragging) {
            isDragging = false;
            startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
        }
    }*/

    /*@Override
    protected boolean onMouseDragged(int mouseX, int mouseY, double deltaX, double deltaY) {
        *//*if (barY + deltaY >= y && barY - y + deltaY <= getMaxDragLength()) {
            draggingY += deltaY;
        }
        if (mouseY == draggingY) {
            window.scrollDirect(transformPosToAmount((float) deltaY));
        }*//*
        if (mouseY >= getTop() && mouseY <= getBottom()) {
            accDragging += deltaY;
            int i = (int) (accDragging * 2.0);
            if (i != 0) {
                //controller.scrollDirectBy(transformPosToAmount(i / 2.0f));
                accDragging -= i / 2.0f;
            }
        }
        return true;
    }*/

    /**
     * Transform pos to scroll amount
     *
     * @param posDelta relative move (position)
     */
    private float toScrollDelta(float posDelta) {
        posDelta *= maxScroll;
        if (vertical) {
            return posDelta / (getHeight() - thumbLength);
        } else {
            return posDelta / (getWidth() - thumbLength);
        }
    }

    public interface ICallback {

        /**
         * Call when click on scroll bar and not on the thumb
         *
         * @param scrollBar   the scroll bar to callback this method
         * @param scrollDelta calculated scroll amount delta
         */
        void onClickScrollBar(ScrollBar scrollBar, float scrollDelta);

        /**
         * Call when drag the scroll thumb
         *
         * @param scrollBar   the scroll bar to callback this method
         * @param scrollDelta calculated scroll amount delta
         */
        void onDragScrollBar(ScrollBar scrollBar, float scrollDelta);
    }
}
