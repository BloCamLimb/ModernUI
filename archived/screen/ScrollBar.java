/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mcgui;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class encapsulated methods to handle events of and draw the scroll bar.
 * Scrollbar is integrated in the view it's created.
 *
 * @since 1.6
 */
public class ScrollBar {

    // scrollbar masks
    private static final int DRAW_TRACK = 1;
    private static final int DRAW_THUMB = 1 << 1;
    private static final int ALWAYS_DRAW_TRACK = 1 << 2;
    private static final int TRACK_HOVERED = 1 << 3;
    private static final int THUMB_HOVERED = 1 << 4;
    private static final int VERTICAL = 1 << 5;

    private final View mView;
    @Nullable
    private Drawable track;
    //Nonnull
    private Drawable thumb;

    public int flags;

    private float thumbOffset;
    private float scrollRange;

    private int left;
    private int top;
    private int right;
    private int bottom;

    /**
     * Alternative size if not specified in drawable
     * {@link #setAlternativeSize(int)}
     * {@link #getSize()}
     */
    private int altSize;

    /**
     * left, top, right, bottom padding
     * {@link #setPadding(int, int, int, int)}
     */
    private int padding;

    public ScrollBar(View view) {
        mView = view;
        //TODO config for default size
        altSize = 5;
    }

    private void draw(@Nonnull Canvas canvas) {
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

        if ((flags & DRAW_TRACK) != 0 && track != null) {
            track.draw(canvas);
        }
        if ((flags & DRAW_THUMB) != 0) {
            // due to gui scaling, we have to do with float rather than integer
            canvas.save();
            if (isVertical()) {
                canvas.translate(0, thumbOffset);
            } else {
                canvas.translate(thumbOffset, 0);
            }
            thumb.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Set scroll bar parameters, should be called from scroller's listener
     *
     * @param range  scroll range, max scroll amount
     * @param offset scroll offset, current scroll amount
     * @param extent visible range
     */
    public void setParameters(float range, float offset, float extent) {
        boolean drawTrack;
        boolean drawThumb;
        boolean vertical = isVertical();
        if (extent <= 0 || range <= 0) {
            drawTrack = (flags & ALWAYS_DRAW_TRACK) != 0;
            drawThumb = false;
        } else {
            drawTrack = drawThumb = true;
        }
        if (track != null) {
            track.setBounds(left, top, right, bottom);
        }

        final int totalLength;
        final int thickness;
        if (vertical) {
            totalLength = getHeight();
            thickness = getWidth();
        } else {
            totalLength = getWidth();
            thickness = getHeight();
        }

        float preciseLength = totalLength * extent / (range + extent);
        float preciseOffset = (totalLength - preciseLength) * offset / range;

        int thumbLength = Math.round(Math.max(preciseLength, thickness << 1));
        thumbOffset = Math.min(preciseOffset, totalLength - thumbLength);
        scrollRange = range;

        if (drawThumb) {
            if (vertical) {
                thumb.setBounds(left, top, right, top + thumbLength);
            } else {
                thumb.setBounds(left, top, left + thumbLength, bottom);
            }
        }
        if (drawTrack) {
            flags |= DRAW_TRACK;
        } else {
            flags &= ~DRAW_TRACK;
        }
        if (drawThumb) {
            flags |= DRAW_THUMB;
        } else {
            flags &= ~DRAW_THUMB;
        }
    }

    public void setTrackDrawable(@Nullable Drawable track) {
        this.track = track;
    }

    public void setThumbDrawable(@Nonnull Drawable thumb) {
        this.thumb = thumb;
    }

    public boolean isAlwaysDrawTrack() {
        return (flags & ALWAYS_DRAW_TRACK) != 0;
    }

    /**
     * Indicates whether the vertical scrollbar track should always be drawn
     * regardless of the extent.
     */
    public void setAlwaysDrawTrack(boolean alwaysDrawTrack) {
        if (alwaysDrawTrack) {
            flags |= ALWAYS_DRAW_TRACK;
        } else {
            flags &= ~ALWAYS_DRAW_TRACK;
        }
    }

    /**
     * Set the scroll bar alternative size, if size is not specified
     * in thumb or track drawable.
     *
     * @param alternativeSize alternative scrollbar thickness
     */
    public void setAlternativeSize(int alternativeSize) {
        altSize = alternativeSize;
    }

    /**
     * Set the scroll bar padding to view frame
     *
     * @param left   left padding [0-255]
     * @param top    top padding [0-255]
     * @param right  right padding [0-255]
     * @param bottom bottom padding [0-255]
     */
    public void setPadding(int left, int top, int right, int bottom) {
        padding = left | top << 8 | right << 16 | bottom << 24;
    }

    public int getLeftPadding() {
        return padding & 0xff;
    }

    public int getTopPadding() {
        return (padding >> 8) & 0xff;
    }

    public int getRightPadding() {
        return (padding >> 16) & 0xff;
    }

    public int getBottomPadding() {
        return (padding >> 24) & 0xff;
    }

    private int getSize() {
        int s;
        if (isVertical()) {
            if (track != null) {
                s = track.getIntrinsicWidth();
            } else {
                s = thumb.getIntrinsicWidth();
            }
        } else {
            if (track != null) {
                s = track.getIntrinsicHeight();
            } else {
                s = thumb.getIntrinsicHeight();
            }
        }
        if (s <= 0) {
            return altSize;
        }
        return s;
    }

    private boolean isVertical() {
        return (flags & VERTICAL) != 0;
    }

    private int getThumbLength() {
        if (isVertical()) {
            return thumb.getHeight();
        } else {
            return thumb.getWidth();
        }
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

    private void setFrame(int l, int t, int r, int b) {
        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    private boolean updateMouseHover(double mouseX, double mouseY) {
        if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
            flags |= TRACK_HOVERED;
            return true;
        }
        //TODO drawable states
        flags &= ~TRACK_HOVERED;
        return false;
    }

    private boolean isThumbHovered() {
        return (flags & THUMB_HOVERED) != 0;
    }

    private boolean isTrackHovered() {
        return (flags & TRACK_HOVERED) != 0;
    }

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

    /*@Override
    protected void onMouseHoverMoved(double mouseX, double mouseY) {
        super.onMouseHoverMoved(mouseX, mouseY);
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
    }*/

    private int getWidth() {
        return right - left;
    }

    private int getHeight() {
        return bottom - top;
    }

    private boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        /*if (thumbHovered) {
            UIManager.INSTANCE.setDragging(this);
            return true;
        }*/
        if (isTrackHovered()) {
            if (isVertical()) {
                float start = top + thumbOffset;
                float end = start + getThumbLength();
                if (mouseY < start) {
                    float delta = toScrollDelta((float) (mouseY - start - 1), true);
                    mView.onScrollBarClicked(true, Math.max(-60.0f, delta));
                    return true;
                } else if (mouseY > end) {
                    float delta = toScrollDelta((float) (mouseY - end + 1), true);
                    mView.onScrollBarClicked(true, Math.min(60.0f, delta));
                    return true;
                }
            } else {
                float start = left + thumbOffset;
                float end = start + getThumbLength();
                if (mouseX < start) {
                    float delta = toScrollDelta((float) (mouseX - start - 1), false);
                    mView.onScrollBarClicked(false, Math.max(-60.0f, delta));
                    return true;
                } else if (mouseX > end) {
                    float delta = toScrollDelta((float) (mouseX - end + 1), false);
                    mView.onScrollBarClicked(false, Math.min(60.0f, delta));
                    return true;
                }
            }
        }
        return false;
    }

    /*@Override
    protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (vertical) {
            if (mouseY >= getTop() && mouseY <= getBottom()) {
                *//*accDelta += deltaY;
                int i = (int) (accDelta * scale);
                float delta = i / scale;
                accDelta -= delta;
                delta = toScrollDelta(delta);*//*
                onScrollBarDragged(toScrollDelta((float) deltaY), vertical);
                return true;
            }
        } else {
            if (mouseX >= getLeft() && mouseX <= getRight()) {
                *//*accDelta += deltaX;
                int i = (int) (accDelta * 2.0f);
                float delta = i / 2.0f;
                accDelta -= delta;
                delta = toScrollDelta(delta);*//*
                onScrollBarDragged(toScrollDelta((float) deltaX), vertical);
                return true;
            }
        }
        return super.onMouseDragged(mouseX, mouseY, deltaX, deltaY);
    }*/

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
     * Transform mouse position change to scroll amount change
     *
     * @param delta    relative mouse position change
     * @param vertical is vertical
     * @return scroll delta
     */
    private float toScrollDelta(float delta, boolean vertical) {
        delta *= scrollRange;
        if (vertical) {
            return delta / (getHeight() - getThumbLength());
        } else {
            return delta / (getWidth() - getThumbLength());
        }
    }
}
