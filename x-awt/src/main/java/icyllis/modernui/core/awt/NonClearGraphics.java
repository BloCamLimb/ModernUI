/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.awt;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * Wrapper for a {@link Graphics} object that delegates to it.
 * Only {@link NonClearGraphics#clearRect(int, int, int, int)} does
 * not delegate in order to prevent clearing the Canvas after
 * it was rendered.
 *
 * @author hageldave
 * @see NonClearGraphics2D
 */
public class NonClearGraphics extends Graphics {

    protected Graphics delegate;

    public NonClearGraphics(Graphics delegate) {
        this.delegate = delegate;
    }

    /**
     * Does nothing. This is to prevent a clearRect call from
     * clearing the already rendered Canvas.
     */
    public void clearRect(int x, int y, int width, int height) {
        // NOOP
    }

    @Deprecated
    public Rectangle getClipRect() {
        return delegate.getClipRect();
    }

    public NonClearGraphics create() {
        return new NonClearGraphics(delegate.create());
    }

    public NonClearGraphics create(int x, int y, int width, int height) {
        return new NonClearGraphics(delegate.create(x, y, width, height));
    }

    public void translate(int x, int y) {
        delegate.translate(x, y);
    }

    public Color getColor() {
        return delegate.getColor();
    }

    public void setColor(Color c) {
        delegate.setColor(c);
    }

    public void setPaintMode() {
        delegate.setPaintMode();
    }

    public void setXORMode(Color c1) {
        delegate.setXORMode(c1);
    }

    public Font getFont() {
        return delegate.getFont();
    }

    public void setFont(Font font) {
        delegate.setFont(font);
    }

    public FontMetrics getFontMetrics() {
        return delegate.getFontMetrics();
    }

    public FontMetrics getFontMetrics(Font f) {
        return delegate.getFontMetrics(f);
    }

    public Rectangle getClipBounds() {
        return delegate.getClipBounds();
    }

    public void clipRect(int x, int y, int width, int height) {
        delegate.clipRect(x, y, width, height);
    }

    public void setClip(int x, int y, int width, int height) {
        delegate.setClip(x, y, width, height);
    }

    public Shape getClip() {
        return delegate.getClip();
    }

    public void setClip(Shape clip) {
        delegate.setClip(clip);
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        delegate.copyArea(x, y, width, height, dx, dy);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        delegate.drawLine(x1, y1, x2, y2);
    }

    public void fillRect(int x, int y, int width, int height) {
        delegate.fillRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height) {
        delegate.drawRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.draw3DRect(x, y, width, height, raised);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.fill3DRect(x, y, width, height, raised);
    }

    public void drawOval(int x, int y, int width, int height) {
        delegate.drawOval(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height) {
        delegate.fillOval(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(Polygon p) {
        delegate.drawPolygon(p);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void fillPolygon(Polygon p) {
        delegate.fillPolygon(p);
    }

    public void drawString(String str, int x, int y) {
        delegate.drawString(str, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        delegate.drawString(iterator, x, y);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        delegate.drawChars(data, offset, length, x, y);
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        delegate.drawBytes(data, offset, length, x, y);
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return delegate.drawImage(img, x, y, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, x, y, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    public void dispose() {
        delegate.dispose();
    }

    public void finalize() {
        delegate.finalize();
    }

    public String toString() {
        return delegate.toString();
    }

    public boolean hitClip(int x, int y, int width, int height) {
        return delegate.hitClip(x, y, width, height);
    }

    public Rectangle getClipBounds(Rectangle r) {
        return delegate.getClipBounds(r);
    }

}
