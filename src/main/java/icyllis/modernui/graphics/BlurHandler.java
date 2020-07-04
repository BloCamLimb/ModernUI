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

package icyllis.modernui.graphics;

import icyllis.modernui.system.Config;
import icyllis.modernui.ui.master.ModernContainerScreen;
import icyllis.modernui.ui.master.ModernScreen;
import icyllis.modernui.ui.master.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderDefault;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Better than Blur (mod) in every respect, and surely they are incompatible
 */
@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    private final ResourceLocation shader = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final List<Class<?>> exclusions = new ArrayList<>();

    /**
     * If is playing animation
     */
    private boolean changingProgress = false;

    /**
     * If blur shader is activated
     */
    private boolean blurring = false;

    /**
     * If a gui excluded, the other guis that opened after this gui won't be blurred, unless current gui closed
     */
    private boolean guiOpened = false;

    /**
     * Background alpha
     */
    private float backgroundAlpha = 0;

    BlurHandler() {

    }

    /**
     * Use blur shader in game renderer post-processing
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    void gGuiOpen(@Nonnull GuiOpenEvent event) {
        @Nullable Screen gui = event.getGui();

        boolean excluded = gui != null && !(gui instanceof ModernScreen) && !(gui instanceof ModernContainerScreen<?>) &&
                exclusions.stream().anyMatch(c -> c.isAssignableFrom(gui.getClass()));

        if (excluded || !Config.CLIENT.blurScreenBackground) {
            backgroundAlpha = 0.5f;
            if (excluded && blurring) {
                minecraft.gameRenderer.stopUseShader();
                changingProgress = false;
                blurring = false;
            }
            return;
        }

        boolean toBlur = gui != null;
        if (minecraft.world != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (toBlur && !blurring && !guiOpened) {
                if (gr.getShaderGroup() == null) {
                    gr.loadShader(shader);
                    changingProgress = true;
                    blurring = true;
                    backgroundAlpha = 0;
                }
            } else if (!toBlur && blurring) {
                gr.stopUseShader();
                changingProgress = false;
                blurring = false;
            }
        }
        guiOpened = toBlur;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in in-game menu
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by Modern UI screen
        if (!Config.CLIENT.blurScreenBackground) {
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
        if (changingProgress && event.phase == TickEvent.Phase.END) {
            float p = Math.min(UIManager.INSTANCE.getDrawingTime(), 200) / 50.0f;
            updateProgress(p);
            if (backgroundAlpha < 0.5f) {
                backgroundAlpha = p / 8.0f;
            }
            if (p == 4.0f) {
                changingProgress = false;
            }
        }
    }

    private void updateProgress(float value) {
        ShaderGroup sg = minecraft.gameRenderer.getShaderGroup();
        if (sg == null)
            return;
        List<Shader> shaders = sg.listShaders;
        for (Shader s : shaders) {
            ShaderDefault u = s.getShaderManager().getShaderUniform("Progress");
            u.set(value);
        }
    }

    public float getBackgroundAlpha() {
        return backgroundAlpha;
    }

}
