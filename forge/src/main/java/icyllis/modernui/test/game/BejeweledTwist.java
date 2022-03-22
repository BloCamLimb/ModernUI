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

package icyllis.modernui.test.game;

import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.audio.AudioManager;
import icyllis.modernui.audio.OggDecoder;
import icyllis.modernui.audio.Track;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.core.MainWindow;
import icyllis.modernui.core.NativeImage;
import icyllis.modernui.core.Window;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.opengl.ShaderManager;
import icyllis.modernui.graphics.opengl.TextureManager;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;
import icyllis.modernui.test.SpectrumGraph;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.util.IntProperty;
import icyllis.modernui.view.Gravity;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Callback;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;

// testing stuff, just a piece of sh*t
public class BejeweledTwist {

    private final Window mWindow;
    private final Thread mRenderThread;

    private double mCursorX;
    private double mCursorY;

    private Track mTrack;
    private SpectrumGraph mSpectrumGraph;

    private final Image mBG;
    private final Rect mBGSrc;
    private final Image mKanna;
    private final RectF mKannaDst = new RectF();

    private int mScore = 0;
    private String mScoreStr = "0";

    private final TextPaint mTextPaint;

    public static void main(String[] args) {
        new BejeweledTwist().run();
    }

    public BejeweledTwist() {
        Thread.currentThread().setName("Main-Thread");
        new ModernUI();
        ArchCore.initialize();
        mWindow = MainWindow.initialize("Bejeweled Twist", 1600, 900);
        try (var c1 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo16x.png");
             var bitmap1 = NativeImage.decode(null, c1);
             var c2 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo32x.png");
             var bitmap2 = NativeImage.decode(null, c2);
             var c3 = ModernUI.getInstance().getResourceChannel(ModernUI.ID, "AppLogo48x.png");
             var bitmap3 = NativeImage.decode(null, c3)) {
            mWindow.setIcon(bitmap1, bitmap2, bitmap3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRenderThread = new Thread(this::runRenderThread, "Render-Thread");

        AudioManager.getInstance().initialize();

        try {
            mTrack = new Track(new OggDecoder(FileChannel.open(Path.of("F:/4.ogg"))));
            mSpectrumGraph = new SpectrumGraph(mTrack, false, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Animator anim = ObjectAnimator.ofInt(this, new IntProperty<>() {
            @Override
            public void setValue(@Nonnull BejeweledTwist object, int value) {
                object.mOffsetY = value;
            }

            @Override
            public Integer get(@Nonnull BejeweledTwist object) {
                return object.mOffsetY;
            }
        }, -600, 0);
        anim.setInterpolator(TimeInterpolator.BOUNCE);
        anim.setDuration(1000);
        mStartAnim = anim;

        mBG = new Image(TextureManager.getInstance().getOrCreate(ModernUI.ID, "test/bg.png",
                TextureManager.CACHE_MASK | TextureManager.MIPMAP_MASK));
        mBGSrc = new Rect(0, 92, mBG.getWidth(), mBG.getHeight() - 92);
        mKanna = new Image(TextureManager.getInstance().getOrCreate(ModernUI.ID, "test/kanna.png",
                TextureManager.CACHE_MASK | TextureManager.MIPMAP_MASK));

        mTextPaint = new TextPaint();
        mTextPaint.setFontSize(32);

        if (mTrack != null) {
            mTrack.play();
        }

        GLFW.glfwSetCursorPosCallback(mWindow.getHandle(), (__, x, y) -> {
            mCursorX = x;
            mCursorY = y;
            if (mDragging != null) {
                if (!mDragging.mDragged) {
                    if (Math.abs(x - mDragging.mDragStartX) > 8) {
                        mDragging.mDragged = true;
                        mDragging.mDragHorizontal = true;
                    } else if (Math.abs(y - mDragging.mDragStartY) > 8) {
                        mDragging.mDragged = true;
                        mDragging.mDragHorizontal = false;
                    }
                    mHovered = null;
                } else if (mHovered != null) {
                    if (conflicts(mDragging.mFruitOption, mHovered.mX, mHovered.mY, false)) {
                        int c = killConflicts(mDragging.mFruitOption, mHovered.mX, mHovered.mY);

                        map[mDragging.mX][mDragging.mY] = mHovered;
                        FruitOption option;
                        do {
                            option = FruitOption.generate();
                        } while (conflicts(option, mHovered.mX, mHovered.mY, true));
                        map[mHovered.mX][mHovered.mY] = new FruitInstance(option, mHovered.mX, mHovered.mY);

                        mHovered.mX = mDragging.mX;
                        mHovered.mY = mDragging.mY;

                        mScore += c * 100;
                        mScoreStr = Integer.toString(mScore);
                    }
                    mDragging.mDragged = false;
                    mDragging = null;
                }
            }
        });
        GLFW.glfwSetKeyCallback(mWindow.getHandle(), (__, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(mWindow.getHandle(), true);
                    case GLFW_KEY_K -> startNewGame();
                }
            }
        });
        GLFW.glfwSetMouseButtonCallback(mWindow.getHandle(), (__, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    mDragging = mHovered;
                    if (mDragging != null) {
                        mDragging.mDragStartX = mCursorX;
                        mDragging.mDragStartY = mCursorY;
                    }
                } else {
                    if (mDragging != null) {
                        mDragging.mDragged = false;
                    }
                    mDragging = null;
                }
            }
        });
    }

    private void run() {
        try {
            mRenderThread.start();
            startNewGame();
            while (!mWindow.shouldClose()) {
                glfwWaitEventsTimeout(1 / 288D);
            }
            mRenderThread.interrupt();
        } finally {
            mWindow.close();
            AudioManager.getInstance().close();
            Stream.of(glfwSetMonitorCallback(null),
                            glfwSetErrorCallback(null))
                    .filter(Objects::nonNull)
                    .forEach(Callback::free);
            glfwTerminate();
        }
    }

    private void runRenderThread() {
        mWindow.makeCurrent();
        ArchCore.initOpenGL();
        GLSurfaceCanvas canvas = GLSurfaceCanvas.initialize();
        ShaderManager.getInstance().reload();
        GLFW.glfwSwapInterval(1);

        Matrix4 projection = new Matrix4();
        Rect screenRect = new Rect();

        GLCore.glEnable(GL_CULL_FACE);
        GLCore.glEnable(GL_BLEND);
        GLCore.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        GLCore.glEnable(GL_STENCIL_TEST);
        GLCore.glEnable(GL_MULTISAMPLE);
        GLCore.glDisable(GL_DEPTH_TEST);

        long lastTime = ArchCore.timeMillis();
        while (!mWindow.shouldClose()) {
            long time = ArchCore.timeMillis();
            long delta = time - lastTime;
            lastTime = time;
            GLCore.resetFrame(mWindow);

            //animationHandler.accept(time);
            if (mWindow.getWidth() > 0) {
                canvas.reset(mWindow.getWidth(), mWindow.getHeight());
                screenRect.set(0, 0, mWindow.getWidth(), mWindow.getHeight());
                Paint paint = Paint.take();
                paint.setRGB(160, 160, 160);
                canvas.drawImage(mBG, mBGSrc, screenRect, paint);
                tickDraw(canvas, delta);

                // render thread, wait UI thread
                canvas.draw(null);
            }

            mWindow.swapBuffers();
        }
    }

    private void tickDraw(Canvas canvas, long deltaMillis) {
        float cx = mWindow.getWidth() / 2f, cy = mWindow.getHeight() / 2f;

        Paint paint = Paint.take();
        float frac = MathUtil.clamp((float) ((mCursorY - mWindow.getHeight() + 250) / 250), 0, 1);
        int alpha = (int) MathUtil.lerp(frac, 0x66, 0xFF);
        paint.setColors(0x20000000, 0x20000000, alpha << 24, alpha << 24);
        canvas.drawRect(0, mWindow.getHeight() - 250, mWindow.getWidth(), mWindow.getHeight(), paint);
        paint.setColor(0x66000000);
        canvas.drawRect(cx - 100, 0, cx + 100, 90, paint);
        if (mSpectrumGraph != null) {
            mSpectrumGraph.update(deltaMillis);
            mSpectrumGraph.draw(canvas, cx, mWindow.getHeight());
        }

        mKannaDst.set(mWindow.getWidth() - mKanna.getWidth() / 2f, cy - 350, mWindow.getWidth(), cy + 350);
        canvas.drawImage(mKanna, null, mKannaDst, null);
        drawScene(canvas, cx, cy, mCursorX, mCursorY);

        canvas.drawText(mScoreStr, 0, mScoreStr.length(), cx, 50, Gravity.CENTER_HORIZONTAL, mTextPaint);
    }

    public final FruitInstance[][] map = new FruitInstance[9][6];

    private final Animator mStartAnim;
    private int mOffsetY;

    private void startNewGame() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {
                FruitOption option;
                do {
                    option = FruitOption.generate();
                } while (conflicts(option, i, j, true));
                map[i][j] = new FruitInstance(option, i, j);
            }
        }
        mStartAnim.start();
    }

    private boolean conflicts(FruitOption base, int x, int y, boolean desire) {
        int count = 0;
        int v = x;
        while (--v >= 0) {
            if (map[v][y] != null && map[v][y].mFruitOption == base) {
                count++;
            } else {
                break;
            }
        }
        v = x;
        while (++v < 9) {
            if (map[v][y] != null && map[v][y].mFruitOption == base) {
                count++;
            } else {
                break;
            }
        }
        v = y;
        while (--v >= 0) {
            if (map[x][v] != null && map[x][v].mFruitOption == base) {
                count++;
            } else {
                break;
            }
        }
        v = y;
        while (++v < 6) {
            if (map[x][v] != null && map[x][v].mFruitOption == base) {
                count++;
            } else {
                break;
            }
        }
        return desire ? count > 1 : count > 2;
    }

    private int killConflicts(FruitOption base, int x, int y) {
        LongList list = new LongArrayList();
        int v = x;
        while (--v >= 0) {
            if (map[v][y] != null && map[v][y].mFruitOption == base) {
                list.add((long) v << 32 | y);
            } else {
                break;
            }
        }
        v = x;
        while (++v < 9) {
            if (map[v][y] != null && map[v][y].mFruitOption == base) {
                list.add((long) v << 32 | y);
            } else {
                break;
            }
        }
        v = y;
        while (--v >= 0) {
            if (map[x][v] != null && map[x][v].mFruitOption == base) {
                list.add((long) x << 32 | v);
            } else {
                break;
            }
        }
        v = y;
        while (++v < 6) {
            if (map[x][v] != null && map[x][v].mFruitOption == base) {
                list.add((long) x << 32 | v);
            } else {
                break;
            }
        }

        for (long pos : list) {
            int i = (int) (pos >> 32);
            int j = (int) (pos);
            FruitOption option;
            do {
                option = FruitOption.generate();
            } while (conflicts(option, i, j, true));
            map[i][j] = new FruitInstance(option, i, j);
        }
        return list.size();
    }

    private FruitInstance mHovered;
    private FruitInstance mDragging;

    public void drawScene(Canvas canvas, float cx, float cy, double mx, double my) {
        float x;
        float y = cy - 290 + mOffsetY;
        FruitInstance hovered = null;
        for (int i = 0; i < 6; i++) {
            x = cx - 440;
            for (int j = 0; j < 9; j++) {
                FruitInstance instance = map[j][i];
                Image image = instance.mFruitOption.mImage;
                float realX = x;
                float realY = y;
                if (instance.mDragged) {
                    if (instance.mDragHorizontal) {
                        realX += mCursorX - instance.mDragStartX;
                    } else {
                        realY += mCursorY - instance.mDragStartY;
                    }
                }
                canvas.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), realX, realY, realX + 80,
                        realY + 80, null);
                if (instance.mDragged || (mx >= x && mx < x + 80 && my >= y && my < y + 80)) {
                    Paint paint = Paint.take();
                    paint.setAlpha(128);
                    canvas.drawRoundRect(realX, realY, realX + 80, realY + 80, 20, paint);
                    if (!instance.mDragged) {
                        hovered = instance;
                    }
                }
                x += 100;
            }
            y += 100;
        }
        mHovered = hovered;
    }

    public enum FruitOption {
        tangerine("tangerine_1f34a"),
        grapes("grapes_1f347"),
        melon("melon_1f348"),
        watermelon("watermelon_1f349"),
        pineapple("pineapple_1f34d"),
        peach("peach_1f351");

        public static final Random random = new Random();

        private static final FruitOption[] VALUES = values();

        public final Image mImage;

        FruitOption(String name) {
            mImage = new Image(TextureManager.getInstance().getOrCreate(ModernUI.ID, "test/" + name + ".png",
                    TextureManager.CACHE_MASK | TextureManager.MIPMAP_MASK));
        }

        static FruitOption generate() {
            return VALUES[random.nextInt(VALUES.length)];
        }
    }

    public static class FruitInstance {

        private final FruitOption mFruitOption;

        private int mX;
        private int mY;

        private double mDragStartX;
        private double mDragStartY;

        private boolean mDragged;
        private boolean mDragHorizontal;

        public FruitInstance(FruitOption fruitOption, int x, int y) {
            mFruitOption = fruitOption;
            mX = x;
            mY = y;
        }
    }
}
