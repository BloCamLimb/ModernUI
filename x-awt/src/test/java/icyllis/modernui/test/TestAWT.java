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

package icyllis.modernui.test;

import icyllis.modernui.core.awt.AWTGLCanvas;
import icyllis.modernui.core.awt.GLData;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.io.Serial;
import java.text.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.opengl.GL11.*;

public class TestAWT {

    static class Requests implements InputMethodRequests {

        final Component mComponent;

        private Requests(Component component) {
            mComponent = component;
        }

        @Override
        public Rectangle getTextLocation(TextHitInfo offset) {
            var r = new Rectangle(0, 0, 200, 20);
            var p = mComponent.getLocationOnScreen();
            r.translate(p.x, p.y);
            return r;
        }

        @Override
        public TextHitInfo getLocationOffset(int x, int y) {
            return TextHitInfo.leading(0);
        }

        @Override
        public int getInsertPositionOffset() {
            return 0;
        }

        @Override
        public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex,
                                                            AttributedCharacterIterator.Attribute[] attributes) {
            return new AttributedString("").getIterator();
        }

        @Override
        public int getCommittedTextLength() {
            return 0;
        }

        @Override
        public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
            return null;
        }

        @Override
        public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
            return null;
        }
    }

    abstract static class AWTGLCanvasExplicitDispose extends AWTGLCanvas {
        protected AWTGLCanvasExplicitDispose(GLData data) {
            super(data);
        }

        @Override
        public void disposeCanvas() {
        }

        public void doDisposeCanvas() {
            super.disposeCanvas();
        }
    }

    public static void main(String[] args) {
        Semaphore signalTerminate = new Semaphore(0);
        Semaphore signalTerminated = new Semaphore(0);
        JFrame frame = new JFrame("Modern UI") {
            @Override
            public void dispose() {
                // request the cleanup
                signalTerminate.release();
                try {
                    // wait until the thread is done with the cleanup
                    signalTerminated.acquire();
                } catch (InterruptedException ignored) {
                }
                super.dispose();
            }
        };

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(600, 600));
        GLData data = new GLData();
        data.profile = GLData.Profile.CORE;
        data.majorVersion = 4;
        data.swapInterval = 0;
        AWTGLCanvasExplicitDispose canvas;
        frame.add(canvas = new AWTGLCanvasExplicitDispose(data) {
            @Serial
            private static final long serialVersionUID = 1L;

            final InputMethodRequests requests = new Requests(this);

            @Override
            public InputMethodRequests getInputMethodRequests() {
                return requests;
            }

            @Override
            public void initGL() {
                System.out.println("OpenGL version: " + effective.majorVersion + "." + effective.minorVersion + " " +
                        "(Profile: " + effective.profile + ")");
                GL.createCapabilities();
            }

            @Override
            public void paintGL() {
                int w = getFramebufferWidth();
                int h = getFramebufferHeight();
                float aspect = (float) w / h;
                double now = System.currentTimeMillis() * 0.001;
                float width = (float) Math.abs(Math.sin(now * 0.3));
                glClearColor(0.3f, 0.4f, 0.5f, 1);
                glClear(GL_COLOR_BUFFER_BIT);
                glViewport(0, 0, w, h);
                swapBuffers();
            }

        }, BorderLayout.CENTER);
        canvas.addInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodTextChanged(InputMethodEvent event) {
                var it = event.getText();
                System.out.println(Thread.currentThread() + " Text Changed " + it);
                if (it != null) {
                    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
                        System.out.printf("\\u%04X", (int) c);
                    }
                    System.out.println();
                    System.out.println(it.getAttributes());
                }
            }

            @Override
            public void caretPositionChanged(InputMethodEvent event) {
                System.out.println("Position Changed");
            }
        });
        canvas.addKeyListener(new KeyAdapter() {
        });
        frame.pack();
        frame.setVisible(true);
        canvas.transferFocus();

        Runnable renderLoop = () -> {
            while (true) {
                canvas.render();
                try {
                    if (signalTerminate.tryAcquire(66, TimeUnit.MILLISECONDS)) {
                        GL.setCapabilities(null);
                        canvas.doDisposeCanvas();
                        signalTerminated.release();
                        return;
                    }
                } catch (InterruptedException ignored) {
                }
            }
        };
        Thread renderThread = new Thread(renderLoop);
        renderThread.start();
    }
}
