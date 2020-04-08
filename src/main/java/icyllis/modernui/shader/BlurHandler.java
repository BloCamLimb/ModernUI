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

package icyllis.modernui.shader;

import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderDefault;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    private final ResourceLocation BLUR = new ResourceLocation("shaders/post/blur_fast.json");

    private boolean changingProgress = false;

    private boolean blurring = false;

    BlurHandler() {

    }

    public void blur(boolean hasGui) {
        if (Minecraft.getInstance().world != null) {
            GameRenderer gr = Minecraft.getInstance().gameRenderer;
            if (hasGui && !blurring) {
                if (gr.getShaderGroup() == null) {
                    gr.loadShader(BLUR);
                    changingProgress = true;
                    blurring = true;
                }
            } else if (!hasGui && blurring) {
                gr.stopUseShader();
                changingProgress = false;
                blurring = false;
            }
        }
    }

    public void tick() {
        if (changingProgress) {
            float p = Math.min(GlobalModuleManager.INSTANCE.getAnimationTime(), 4.0f);
            this.updateProgress(p);
            if (p == 4.0f) {
                changingProgress = false;
            }
        }
    }

    private void updateProgress(float value) {
        ShaderGroup sg = Minecraft.getInstance().gameRenderer.getShaderGroup();
        if (sg == null)
            return;
        List<Shader> shaders = sg.listShaders;
        for (Shader s : shaders) {
            ShaderDefault u = s.getShaderManager().getShaderUniform("Progress");
            u.set(value);
        }
    }

}
