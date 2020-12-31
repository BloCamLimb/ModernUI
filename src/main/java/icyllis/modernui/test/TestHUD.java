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

package icyllis.modernui.test;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.text.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.mixin.AccessFoodData;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.model.animation.Animation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class TestHUD {

    //private static DecimalFormat decimalFormat = new DecimalFormat("#.00");

    public static void drawHUD(@Nonnull Canvas canvas) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        RenderSystem.disableDepthTest();

        Minecraft minecraft = Minecraft.getInstance();
        MainWindow mainWindow = minecraft.getMainWindow();
        float aspectRatio = (float) mainWindow.getFramebufferWidth() / mainWindow.getFramebufferHeight();
        RenderSystem.multMatrix(Matrix4f.perspective(90.0, aspectRatio, 1.0f, 100.0f));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(-1.58f * aspectRatio, -1.0f, -1.8f);
        GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
        GL11.glRotatef(12, 0, 1, 0);

        PlayerEntity player = (PlayerEntity) minecraft.getRenderViewEntity();

        float partialTicks = Animation.getPartialTickTime();

        float f = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        float f1 = -(player.distanceWalkedModified + f * partialTicks);
        float f2 = MathHelper.lerp(partialTicks, player.prevCameraYaw, player.cameraYaw);
        GL11.glTranslatef(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5f,
                Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0f);
        GL11.glRotatef(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0f, 0, 0, 1);
        GL11.glRotatef(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2f) * f2) * 5.0f, 1, 0, 0);

        canvas.setColor(255, 19, 19, 128);
        canvas.drawRoundedRect(0, 25, player.getHealth() * 140 / player.getMaxHealth(), 37, 6);
        canvas.setColor(86, 184, 255, 128);
        canvas.drawRoundedRect(0, 11, player.getAir() * 140f / player.getMaxAir(), 23, 6);
        canvas.setColor(184, 132, 88, 128);
        FoodStats foodStats = player.getFoodStats();
        canvas.drawRoundedRect(0, -3, foodStats.getFoodLevel() * 7, 9, 6);

        canvas.resetColor();
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(String.format("%.2f / %.2f", player.getHealth(), player.getMaxHealth()), 70, 27);
        canvas.drawText(String.format("%d / %d", player.getAir(), player.getMaxAir()), 70, 13);
        canvas.drawText(String.format("%d / %.2f / %.2f", foodStats.getFoodLevel(), foodStats.getSaturationLevel(),
                ((AccessFoodData) foodStats).getFoodExhaustionLevel()), 70, -1);

        RenderSystem.enableDepthTest();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();

        minecraft.textureManager.bindTexture(AbstractGui.GUI_ICONS_LOCATION);
    }
}
