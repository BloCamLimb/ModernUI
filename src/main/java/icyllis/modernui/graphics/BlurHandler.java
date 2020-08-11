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

package icyllis.modernui.graphics;

import icyllis.modernui.ui.master.ModernContainerScreen;
import icyllis.modernui.ui.master.ModernScreen;
import icyllis.modernui.ui.master.UIManager;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DownloadTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderDefault;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Better than Blur (mod) in every respect, and surely they are incompatible
 */
@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    /**
     * Config value
     *
     * @see icyllis.modernui.system.Config.Client
     */
    public static boolean sBlurEffect;
    public static float   sAnimationDuration;
    public static float   sBlurRadius;
    public static float   sBackgroundAlpha;

    private final ResourceLocation shader = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final Set<Class<?>> exclusions = new ObjectArraySet<>();

    /**
     * If is playing animation
     */
    private boolean changingProgress;

    /**
     * If blur shader is activated
     */
    private boolean blurring;

    /**
     * If a gui excluded, the other guis that opened after this gui won't be blurred, unless current gui closed
     */
    private boolean guiOpened;

    /**
     * Background alpha
     */
    private float backgroundAlpha;

    BlurHandler() {

    }

    /**
     * Use blur shader in game renderer post-processing.
     * Hotfix 1.16, see
     * {@link UIManager#gRenderGameOverlay(RenderGameOverlayEvent.Pre)}
     */
    @SuppressWarnings("JavadocReference")
    @SubscribeEvent(priority = EventPriority.LOW)
    void gGuiOpen(@Nonnull GuiOpenEvent event) {
        @Nullable Screen gui = event.getGui();
        if (minecraft.world == null) {
            return;
        }

        boolean excluded = gui != null && !(gui instanceof ModernScreen)
                && !(gui instanceof ModernContainerScreen<?>)
                && exclusions.stream().anyMatch(c -> c.isAssignableFrom(gui.getClass()));
        boolean notBlur = excluded || !sBlurEffect;
        if (notBlur && excluded && blurring) {
            minecraft.gameRenderer.stopUseShader();
            changingProgress = false;
            blurring = false;
        }

        boolean toBlur = gui != null;
        GameRenderer gr = minecraft.gameRenderer;
        if (toBlur && !blurring && !guiOpened) {
            if (!notBlur && gr.getShaderGroup() == null) {
                gr.loadShader(shader);
                blurring = true;
                if (sAnimationDuration <= 0) {
                    updateRadius(sBlurRadius);
                }
            }
            if (sAnimationDuration > 0) {
                changingProgress = true;
                backgroundAlpha = 0;
            } else {
                changingProgress = false;
                backgroundAlpha = sBackgroundAlpha;
            }
        } else if (!toBlur && blurring) {
            gr.stopUseShader();
            changingProgress = false;
            blurring = false;
        }
        guiOpened = toBlur;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in in-game menu
     *
     * @see ModernScreen#init(Minecraft, int, int)
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by opened ModernScreen
        if (!sBlurEffect) {
            return;
        }
        if (minecraft.world != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.getShaderGroup() == null) {
                gr.loadShader(shader);
                changingProgress = true;
                blurring = true;
            }
        }
    }

    public void loadExclusions(@Nonnull List<? extends String> names) {
        exclusions.clear();
        exclusions.add(ChatScreen.class);
        exclusions.add(DownloadTerrainScreen.class);
        for (String s : names) {
            try {
                Class<?> clazz = Class.forName(s);
                exclusions.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    void gRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (changingProgress && event.phase == TickEvent.Phase.START) {
            float p = Math.min(UIManager.getInstance().getDrawingTime() / sAnimationDuration, 1.0f);
            if (blurring) {
                updateRadius(p * sBlurRadius);
            }
            if (backgroundAlpha < sBackgroundAlpha) {
                backgroundAlpha = p * sBackgroundAlpha;
            }
            if (p == 1.0f) {
                changingProgress = false;
            }
        }
    }

    private void updateRadius(float radius) {
        ShaderGroup sg = minecraft.gameRenderer.getShaderGroup();
        if (sg == null)
            return;
        List<Shader> shaders = sg.listShaders;
        for (Shader s : shaders) {
            ShaderDefault u = s.getShaderManager().getShaderUniform("Progress");
            u.set(radius);
        }
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }
}
