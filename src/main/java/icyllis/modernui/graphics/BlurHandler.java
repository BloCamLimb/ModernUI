/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.system.ConfigManager;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    private final ResourceLocation BLUR = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final List<Class<?>> excludedClass = new ArrayList<>();

    /** If is playing animation **/
    private boolean changingProgress = false;

    /** If blur shader is activated **/
    private boolean blurring = false;

    /** If a gui excluded, the other guis that opened after this gui won't be blurred, unless current gui closed **/
    private boolean guiOpened = false;

    /** Background alpha **/
    private float backAlpha = 0;

    BlurHandler() {

    }

    public void blur(@Nullable Screen gui) {
        boolean excluded = gui != null && excludedClass.stream().anyMatch(c -> c.isAssignableFrom(gui.getClass()));
        if (excluded || !ConfigManager.CLIENT.blurScreenBackground) {
            backAlpha = 0.5f;
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
                    gr.loadShader(BLUR);
                    changingProgress = true;
                    blurring = true;
                    backAlpha = 0;
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
     * Mainly for ingame menu gui, re-blur after resources reloaded
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by modern ui screen
        if (!ConfigManager.CLIENT.blurScreenBackground) {
            return;
        }
        if (minecraft.world != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.getShaderGroup() == null) {
                gr.loadShader(BLUR);
                changingProgress = true;
                blurring = true;
            }
        }
    }

    public void loadExclusions(@Nonnull List<? extends String> names) {
        excludedClass.clear();
        excludedClass.add(ChatScreen.class);
        for (String s : names) {
            try {
                Class<?> clazz = Class.forName(s);
                excludedClass.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void renderTick() {
        if (changingProgress) {
            float p = Math.min(UIManager.INSTANCE.getAnimationTime(), 4.0f);
            this.updateProgress(p);
            if (backAlpha != 0.5f) {
                backAlpha = p / 8f;
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
        return backAlpha;
    }

}
