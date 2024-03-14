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

import org.lwjgl.system.Platform;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Callable;

/**
 * An AWT {@link Canvas} that supports to be drawn on using OpenGL.
 *
 * @author Kai Burjack
 */
public abstract class AWTGLCanvas extends Canvas {
    private static final long serialVersionUID = 1L;
    protected PlatformGLCanvas platformCanvas = createPlatformCanvas();

    private static PlatformGLCanvas createPlatformCanvas() {
        switch (Platform.get()) {
            case WINDOWS:
                return new PlatformWin32GLCanvas();
            case LINUX:
                return new PlatformLinuxGLCanvas();
            case MACOSX:
                return new PlatformMacOSXGLCanvas();
            default:
                throw new UnsupportedOperationException("Platform " + Platform.get() + " not yet supported");
        }
    }

    protected long context;
    protected final GLData data;
    protected final GLData effective = new GLData();
    protected boolean initCalled;
    private int framebufferWidth, framebufferHeight;
    private final ComponentListener listener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            java.awt.geom.AffineTransform t = AWTGLCanvas.this.getGraphicsConfiguration().getDefaultTransform();
            float sx = (float) t.getScaleX(), sy = (float) t.getScaleY();
            AWTGLCanvas.this.framebufferWidth = (int) (getWidth() * sx);
            AWTGLCanvas.this.framebufferHeight = (int) (getHeight() * sy);
        }
    };

    @Override
    public void removeNotify() {
        super.removeNotify();
        // prepare for a possible re-adding
        context = 0;
        initCalled = false;
        disposeCanvas();
    }

    @Override
    public synchronized void addComponentListener(ComponentListener l) {
        super.addComponentListener(l);
    }

    public void disposeCanvas() {
        this.platformCanvas.dispose();
    }

    protected AWTGLCanvas(GLData data) {
        this.data = data;
        this.addComponentListener(listener);
    }

    protected AWTGLCanvas() {
        this(new GLData());
    }

    protected void beforeRender() {
        if (context == 0L) {
            try {
                context = platformCanvas.create(this, data, effective);
            } catch (AWTException e) {
                throw new RuntimeException("Exception while creating the OpenGL context", e);
            }
        }
        try {
            platformCanvas.lock(); // <- MUST lock on Linux
        } catch (AWTException e) {
            throw new RuntimeException("Failed to lock Canvas", e);
        }
        platformCanvas.makeCurrent(context);
    }

    protected void afterRender() {
        platformCanvas.makeCurrent(0L);
        try {
            platformCanvas.unlock(); // <- MUST unlock on Linux
        } catch (AWTException e) {
            throw new RuntimeException("Failed to unlock Canvas", e);
        }
    }

    public <T> T executeInContext(Callable<T> callable) throws Exception {
        beforeRender();
        try {
            return callable.call();
        } finally {
            afterRender();
        }
    }

    public void runInContext(Runnable runnable) {
        beforeRender();
        try {
            runnable.run();
        } finally {
            afterRender();
        }
    }

    public void render() {
        beforeRender();
        try {
            if (!initCalled) {
                initGL();
                initCalled = true;
            }
            paintGL();
        } finally {
            afterRender();
        }
    }

    /**
     * Will be called once after the OpenGL has been created.
     */
    public abstract void initGL();

    /**
     * Will be called whenever the {@link Canvas} needs to paint itself.
     */
    public abstract void paintGL();

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public final void swapBuffers() {
        platformCanvas.swapBuffers();
    }

    /**
     * Returns Graphics object that ignores {@link Graphics#clearRect(int, int, int, int)}
     * calls.
     * This is done so that the frame buffer will not be cleared by AWT/Swing internals.
     */
    @Override
    public Graphics getGraphics() {
        Graphics graphics = super.getGraphics();
        return (graphics instanceof Graphics2D) ?
                new NonClearGraphics2D((Graphics2D) graphics) : new NonClearGraphics(graphics);
    }

}
