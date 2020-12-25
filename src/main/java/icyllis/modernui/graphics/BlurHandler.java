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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.view.IMuiScreen;
import icyllis.modernui.view.UIManager;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderDefault;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    /**
     * Config values
     *
     * @see icyllis.modernui.system.Config.Client
     */
    public static boolean sBlurEffect;
    public static float sAnimationDuration;
    public static float sBlurRadius;
    public static float sBackgroundAlpha;

    // minecraft namespace
    private final ResourceLocation shader = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final Set<Class<?>> blacklist = new ObjectArraySet<>();

    /**
     * If is playing animation
     */
    private boolean fadingIn;

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
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    void onGuiOpen(@Nonnull GuiOpenEvent event) {
        @Nullable Screen gui = event.getGui();
        if (minecraft.world == null) {
            return;
        }
        final boolean excluded;
        if (gui == null || gui instanceof IMuiScreen) {
            excluded = false;
        } else {
            Class<?> t = gui.getClass();
            excluded = blacklist.stream().anyMatch(c -> c.isAssignableFrom(t));
        }
        boolean blurDisabled = excluded || !sBlurEffect;
        if (blurDisabled && excluded && blurring) {
            minecraft.gameRenderer.stopUseShader();
            fadingIn = false;
            blurring = false;
        }

        boolean hasGui = gui != null;
        GameRenderer gr = minecraft.gameRenderer;
        if (hasGui && !blurring && !guiOpened) {
            if (!blurDisabled && gr.getShaderGroup() == null) {
                gr.loadShader(shader);
                blurring = true;
                if (sAnimationDuration <= 0) {
                    updateRadius(sBlurRadius);
                }
            }
            if (sAnimationDuration > 0) {
                fadingIn = true;
                backgroundAlpha = 0;
            } else {
                fadingIn = false;
                backgroundAlpha = sBackgroundAlpha;
            }
        } else if (!hasGui && blurring) {
            gr.stopUseShader();
            fadingIn = false;
            blurring = false;
        }
        guiOpened = hasGui;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in in-game menu
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by opened ModernUI Screen
        if (!sBlurEffect) {
            return;
        }
        if (minecraft.world != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.getShaderGroup() == null) {
                gr.loadShader(shader);
                fadingIn = true;
                blurring = true;
            }
        }
    }

    public void loadBlacklist(@Nonnull List<? extends String> names) {
        blacklist.clear();
        for (String s : names) {
            try {
                Class<?> clazz = Class.forName(s);
                blacklist.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (fadingIn && event.phase == TickEvent.Phase.START) {
            float p = Math.min(UIManager.getInstance().getDrawingTime() / sAnimationDuration, 1.0f);
            if (blurring) {
                updateRadius(p * sBlurRadius);
            }
            if (backgroundAlpha < sBackgroundAlpha) {
                backgroundAlpha = p * sBackgroundAlpha;
            }
            if (p == 1.0f) {
                fadingIn = false;
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

    public void drawScreenBackground(@Nonnull Screen screen, @Nonnull MatrixStack stack, int x1, int y1, int x2, int y2) {
        int a = (int) (backgroundAlpha * 0xff);
        if (a == 0) {
            return;
        }
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Matrix4f matrix = stack.getLast().getMatrix();
        int z = screen.getBlitOffset();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        builder.pos(matrix, x2, y1, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x1, y1, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x1, y2, z).color(0, 0, 0, a).endVertex();
        builder.pos(matrix, x2, y2, z).color(0, 0, 0, a).endVertex();
        builder.finishDrawing();
        WorldVertexBufferUploader.draw(builder);

        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }
}
