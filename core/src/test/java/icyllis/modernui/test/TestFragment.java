/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import com.ibm.icu.text.CompactDecimalFormat;
import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.audio.*;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.TypedValue;
import icyllis.modernui.view.*;
import icyllis.modernui.view.ViewGroup.LayoutParams;
import icyllis.modernui.widget.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.DoubleStream;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.view.View.dp;

public class TestFragment extends Fragment {

    public static SpectrumGraph sSpectrumGraph;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Configurator.setRootLevel(Level.ALL);
        int complex = TypedValue.floatToComplex(-0.0f);
        LOGGER.info("{} -> {}",
                Integer.toHexString(complex),
                TypedValue.complexToFloat(complex));
        CompactDecimalFormat format = CompactDecimalFormat.getInstance(
                new Locale("zh"), CompactDecimalFormat.CompactStyle.SHORT);
        format.setMaximumFractionDigits(2);
        LOGGER.info(format.format(new BigDecimal("2136541565.615")));
        LOGGER.info("Levenshtein distance: {}", TextUtils.distance("sunday", "saturday"));

        double[] doubles = new double[]{1, 160, 3};
        DoubleStream.of(doubles).average().ifPresent(LOGGER::info);
        LOGGER.info(MathUtil.averageStable(doubles));

        try (ModernUI app = new ModernUI()) {
            app.run(new TestFragment());
        }
        AudioManager.getInstance().close();
        String str = """
                public final class Reference {
                public:
                    static func hash(x: Object?): int {
                        uint* v = (uint*) x;
                        int h = (int) (v ^ (v >>> 32)) * 0x9e3779b1;
                        return h ^ (h >>> 16);
                    }
                    
                    @ForceInline
                    static func equals(a: Object?, b: Object?): bool {
                        return (uint*) a == (uint*) b;
                    }
                }
                public final class Objects {
                public:
                    static func hash(x: Object?): int {
                        return x ? x.hash() : 0;
                    }
                                    
                    static func equals(a: Object?, b: Object?): bool {
                        return Reference::equals(a, b) || (a && b && a.equals(b));
                    }
                }
                """;
    }

    @Override
    public void onAttach() {
        super.onAttach();
        getParentFragmentManager().beginTransaction()
                .setPrimaryNavigationFragment(this)
                .commit();
    }

    @Override
    public void onCreate(@Nullable DataSet savedInstanceState) {
        super.onCreate(savedInstanceState);
        getChildFragmentManager().beginTransaction()
                .replace(660, new FragmentA(), null)
                .commit();

        AudioManager.getInstance().initialize();

        CompletableFuture.runAsync(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                stack.nUTF8("*.ogg", true);
                filters.put(stack.getPointerAddress());
                filters.rewind();
                path = TinyFileDialogs.tinyfd_openFileDialog(null, null, filters, "Ogg Vorbis (*.ogg)", false);
            }
            if (path != null) {
                try {
                    FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.READ);
                    OggDecoder decoder = new OggDecoder(channel);
                    Track track = new Track(decoder);
                    sSpectrumGraph = new SpectrumGraph(track, false, 600);
                    track.play();

                    requireView().postInvalidate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        var base = new ScrollView();
        base.setId(660);

        base.setBackground(new Drawable() {
            long lastTime = AnimationUtils.currentAnimationTimeMillis();

            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint paint = Paint.obtain();
                Rect b = getBounds();
                paint.setRGBA(8, 8, 8, 80);
                canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, 8, paint);

                SpectrumGraph graph = sSpectrumGraph;
                long time = AnimationUtils.currentAnimationTimeMillis();
                long delta = time - lastTime;
                lastTime = time;
                if (graph != null) {
                    graph.update(delta);
                    graph.draw(canvas, getBounds().centerX(), getBounds().centerY());
                    invalidateSelf();
                }
                paint.recycle();
            }
        });
        {
            var params = new FrameLayout.LayoutParams(dp(480), dp(360));
            params.gravity = Gravity.CENTER;
            base.setLayoutParams(params);
        }
        //base.setRotation(30);
        return base;
    }

    public static class FragmentA extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@Nullable ViewGroup container,
                                 @Nullable DataSet savedInstanceState) {
            LinearLayout content = new TestLinearLayout();
            //content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            content.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            content.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEY_E && event.getAction() == KeyEvent.ACTION_UP) {
                    getParentFragmentManager().beginTransaction()
                            .replace(getId(), new FragmentB())
                            .addToBackStack(null)
                            .setReorderingAllowed(true)
                            .commit();
                    return true;
                }
                return false;
            });

            LOGGER.info("FragmentA onCreateView(), id={}", getId());

            return content;
        }
    }

    public static class FragmentB extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@Nullable ViewGroup container,
                                 @Nullable DataSet savedInstanceState) {
            var tv = new TextView();
            tv.setText("My name is Van, I'm an arist, a performance artist.");
            return tv;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LOGGER.info("FragmentB onDestroy()");
        }
    }

    private static class TestView extends View {

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            //canvas.drawRing(100, 20, 5, 8);
            // 3


            /*//RenderHelper.setupGuiFlatDiffuseLighting();
            //GL11.glColor4d(1, 1, 1, 1);
            //GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            RenderSystem.disableDepthTest();

            MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
            RenderSystem.multMatrix(Matrix4f.perspective(90.0D,
                    (float) mainWindow.getFramebufferWidth() / mainWindow.getFramebufferHeight(),
                    1.0F, 100.0F));
            //RenderSystem.viewport(0, 0, mainWindow.getFramebufferWidth(), mainWindow.getFramebufferHeight());
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(-2.8f, -1.0f, -1.8f);
            GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
            //GL11.glTranslatef(0, 3, 1984);
            //GL11.glRotatef((canvas.getDrawingTime() / 10f) % 360 - 180, 0, 1, 0);
            GL11.glRotatef(12, 0, 1, 0);
            *//*if ((canvas.getDrawingTime() ^ 127) % 40 == 0) {
             *//**//*float[] pj = new float[16];
                GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, pj);
                ModernUI.LOGGER.info(Arrays.toString(pj));
                GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, pj);
                ModernUI.LOGGER.info(Arrays.toString(pj));*//**//*
                ModernUI.LOGGER.info(GL11.glGetBoolean(GL30.GL_RESCALE_NORMAL));
            }*//*
            ClientPlayerEntity player = Minecraft.getInstance().player;
            canvas.setColor(170, 220, 240, 128);
            if (player != null) {
                canvas.drawRoundedRect(0, 25, player.getHealth() * 140 / player.getMaxHealth(), 39, 4);
            }
            *//*canvas.setAlpha(255);
            canvas.drawRoundedFrame(1, 26, 141, 40, 4);*//*
             *//*canvas.setColor(53, 159, 210, 192);
            canvas.drawRoundedFrame(0, 25, 140, 39, 4);*//*
            if (player != null) {
                canvas.resetColor();
                canvas.setTextAlign(TextAlign.RIGHT);
                canvas.drawText(decimalFormat.format(player.getHealth()) + " / " + decimalFormat.format(player
                .getMaxHealth()), 137, 28);
            }
            RenderSystem.enableDepthTest();
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            //GL11.glEnable(GL11.GL_CULL_FACE);
            //RenderHelper.setupGui3DDiffuseLighting();

            //canvas.drawRoundedRect(0, 25, 48, 45, 6);*/
        }
    }
}
