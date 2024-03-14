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
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Wrapper for a {@link Graphics2D} object that delegates to it.
 * Only {@link NonClearGraphics2D#clearRect(int, int, int, int)} does
 * not delegate in order to prevent clearing the Canvas after
 * it was rendered.
 *
 * @author hageldave
 * @see NonClearGraphics
 */
public class NonClearGraphics2D extends Graphics2D {

    protected Graphics2D delegate;

    public NonClearGraphics2D(Graphics2D delegate) {
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

    public NonClearGraphics2D create() {
        return new NonClearGraphics2D((Graphics2D) delegate.create());
    }

    public NonClearGraphics2D create(int x, int y, int width, int height) {
        return new NonClearGraphics2D((Graphics2D) delegate.create(x, y, width, height));
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

    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.draw3DRect(x, y, width, height, raised);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        delegate.fill3DRect(x, y, width, height, raised);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void draw(Shape s) {
        delegate.draw(s);
    }

    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return delegate.drawImage(img, xform, obs);
    }

    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        delegate.drawImage(img, op, x, y);
    }

    public void drawOval(int x, int y, int width, int height) {
        delegate.drawOval(x, y, width, height);
    }

    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        delegate.drawRenderedImage(img, xform);
    }

    public void fillOval(int x, int y, int width, int height) {
        delegate.fillOval(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        delegate.drawRenderableImage(img, xform);
    }

    public void drawString(String str, int x, int y) {
        delegate.drawString(str, x, y);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        delegate.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawString(String str, float x, float y) {
        delegate.drawString(str, x, y);
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        delegate.drawString(iterator, x, y);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        delegate.drawString(iterator, x, y);
    }

    public void drawPolygon(Polygon p) {
        delegate.drawPolygon(p);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        delegate.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void drawGlyphVector(GlyphVector g, float x, float y) {
        delegate.drawGlyphVector(g, x, y);
    }

    public void fillPolygon(Polygon p) {
        delegate.fillPolygon(p);
    }

    public void fill(Shape s) {
        delegate.fill(s);
    }

    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return delegate.hit(rect, s, onStroke);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        delegate.drawChars(data, offset, length, x, y);
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return delegate.getDeviceConfiguration();
    }

    public void setComposite(Composite comp) {
        delegate.setComposite(comp);
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        delegate.drawBytes(data, offset, length, x, y);
    }

    public void setPaint(Paint paint) {
        delegate.setPaint(paint);
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return delegate.drawImage(img, x, y, observer);
    }

    public void setStroke(Stroke s) {
        delegate.setStroke(s);
    }

    public void setRenderingHint(Key hintKey, Object hintValue) {
        delegate.setRenderingHint(hintKey, hintValue);
    }

    public Object getRenderingHint(Key hintKey) {
        return delegate.getRenderingHint(hintKey);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, observer);
    }

    public void setRenderingHints(Map<?, ?> hints) {
        delegate.setRenderingHints(hints);
    }

    public void addRenderingHints(Map<?, ?> hints) {
        delegate.addRenderingHints(hints);
    }

    public RenderingHints getRenderingHints() {
        return delegate.getRenderingHints();
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, x, y, bgcolor, observer);
    }

    public void translate(int x, int y) {
        delegate.translate(x, y);
    }

    public void translate(double tx, double ty) {
        delegate.translate(tx, ty);
    }

    public void rotate(double theta) {
        delegate.rotate(theta);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public void rotate(double theta, double x, double y) {
        delegate.rotate(theta, x, y);
    }

    public void scale(double sx, double sy) {
        delegate.scale(sx, sy);
    }

    public void shear(double shx, double shy) {
        delegate.shear(shx, shy);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public void transform(AffineTransform Tx) {
        delegate.transform(Tx);
    }

    public void setTransform(AffineTransform Tx) {
        delegate.setTransform(Tx);
    }

    public AffineTransform getTransform() {
        return delegate.getTransform();
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    public Paint getPaint() {
        return delegate.getPaint();
    }

    public Composite getComposite() {
        return delegate.getComposite();
    }

    public void setBackground(Color color) {
        delegate.setBackground(color);
    }

    public Color getBackground() {
        return delegate.getBackground();
    }

    public Stroke getStroke() {
        return delegate.getStroke();
    }

    public void clip(Shape s) {
        delegate.clip(s);
    }

    public FontRenderContext getFontRenderContext() {
        return delegate.getFontRenderContext();
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
