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

package icyllis.modernui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.ColorEvaluator;
import icyllis.modernui.mixin.AccessGameRenderer;
import icyllis.modernui.mixin.AccessPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    /**
     * Config values
     */
    public static boolean sBlurEffect;
    public static float sAnimationDuration;
    public static float sBlurRadius;
    public static int[] sBackgroundColor = new int[4];

    // minecraft namespace
    private final ResourceLocation mBlurPostEffect = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final Set<Class<?>> mBlacklist = new HashSet<>();

    private final int[] mBackgroundColor = new int[4];

    /**
     * If is playing animation
     */
    private boolean mFadingIn;

    /**
     * If blur post-processing shader is activated
     */
    private boolean mBlurring;

    /**
     * If a screen excluded, the other screens that opened after this screen won't be blurred, unless current screen
     * closed
     */
    private boolean mScreenOpened;

    /**
     * Use blur shader in game renderer post-processing.
     */
    public void count(@Nullable Screen nextScreen) {
        if (minecraft.level == null) {
            return;
        }
        boolean excluded = false;
        if (nextScreen != null && !(nextScreen instanceof MuiScreen)) {
            Class<?> t = nextScreen.getClass();
            if (t != null) {
                for (Class<?> c : mBlacklist) {
                    if (c.isAssignableFrom(t)) {
                        excluded = true;
                        break;
                    }
                }
            }
        }
        boolean blurDisabled = excluded || !sBlurEffect;
        if (blurDisabled && excluded && mBlurring) {
            minecraft.gameRenderer.shutdownEffect();
            mFadingIn = false;
            mBlurring = false;
        }

        boolean hasGui = nextScreen != null;
        GameRenderer gr = minecraft.gameRenderer;
        if (hasGui && !mBlurring && !mScreenOpened) {
            if (!blurDisabled && gr.currentEffect() == null) {
                ((AccessGameRenderer) gr).callLoadEffect(mBlurPostEffect);
                mBlurring = true;
                if (sAnimationDuration <= 0) {
                    updateRadius(sBlurRadius);
                }
            }
            if (sAnimationDuration > 0) {
                mFadingIn = true;
                Arrays.fill(mBackgroundColor, 0);
            } else {
                mFadingIn = false;
                System.arraycopy(sBackgroundColor, 0, mBackgroundColor, 0, 4);
            }
        } else if (!hasGui && mBlurring) {
            gr.shutdownEffect();
            mFadingIn = false;
            mBlurring = false;
        }
        mScreenOpened = hasGui;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in in-game menu
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by opened ModernUI Screen
        if (!sBlurEffect) {
            return;
        }
        if (minecraft.level != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.currentEffect() == null) {
                ((AccessGameRenderer) gr).callLoadEffect(mBlurPostEffect);
                mFadingIn = true;
                mBlurring = true;
            }
        }
    }

    public void loadBlacklist(@Nullable List<? extends String> names) {
        mBlacklist.clear();
        if (names != null) {
            for (String s : names) {
                if (s == null || s.isEmpty()) {
                    continue;
                }
                try {
                    Class<?> clazz = Class.forName(s);
                    mBlacklist.add(clazz);
                } catch (ClassNotFoundException e) {
                    ModernUI.LOGGER.info(ModernUI.MARKER, "Failed to find blur blacklist class: {}", s, e);
                }
            }
        }
    }

    /**
     * Render tick, should called before rendering things
     */
    public void update(long time) {
        if (mFadingIn) {
            float p = Math.min(time / sAnimationDuration, 1.0f);
            if (mBlurring) {
                updateRadius(p * sBlurRadius);
            }
            for (int i = 0; i < 4; i++) {
                mBackgroundColor[i] = ColorEvaluator.evaluate(p, 0, sBackgroundColor[i]);
            }
            if (p == 1.0f) {
                mFadingIn = false;
            }
        }
    }

    private void updateRadius(float radius) {
        PostChain effect = minecraft.gameRenderer.currentEffect();
        if (effect == null)
            return;
        List<PostPass> passes = ((AccessPostChain) effect).getPasses();
        for (PostPass s : passes) {
            s.getEffect().safeGetUniform("Progress").set(radius);
        }
    }

    public void drawScreenBackground(@Nonnull Screen screen, @Nonnull PoseStack stack, int x1, int y1, int x2, int y2) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = stack.last().pose();
        int z = screen.getBlitOffset();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        int color = mBackgroundColor[1];
        builder.vertex(matrix, x2, y1, z).color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24).endVertex();
        color = mBackgroundColor[0];
        builder.vertex(matrix, x1, y1, z).color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24).endVertex();
        color = mBackgroundColor[3];
        builder.vertex(matrix, x1, y2, z).color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24).endVertex();
        color = mBackgroundColor[2];
        builder.vertex(matrix, x2, y2, z).color(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >>> 24).endVertex();
        builder.end();
        BufferUploader.end(builder);

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }
}
