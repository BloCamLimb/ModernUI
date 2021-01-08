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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.ModernFontRenderer;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.system.mixin.AccessFoodData;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.model.animation.Animation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestHUD {

    //private static DecimalFormat decimalFormat = new DecimalFormat("#.00");

    public static void drawBars(@Nonnull Canvas canvas) {
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

        // see ForgeIngameGUI
        PlayerEntity player = (PlayerEntity) minecraft.getRenderViewEntity();
        Objects.requireNonNull(player);

        float partialTicks = Animation.getPartialTickTime();

        float f = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        float f1 = -(player.distanceWalkedModified + f * partialTicks);
        float f2 = MathHelper.lerp(partialTicks, player.prevCameraYaw, player.cameraYaw);
        GL11.glTranslatef(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5f,
                Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0f);
        GL11.glRotatef(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0f, 0, 0, 1);
        GL11.glRotatef(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2f) * f2) * 5.0f, 1, 0, 0);

        canvas.setColor(255, 19, 19, 128);
        float r = Math.min(player.getHealth() * 140 / player.getMaxHealth(), 140);
        canvas.drawRoundedRect(0, 25, r, 37, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, 24, 141, 38, 7);
        canvas.setColor(86, 184, 255, 128);
        r = player.getAir() * 140f / player.getMaxAir();
        canvas.drawRoundedRect(0, 11, r, 23, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, 10, 141, 24, 7);
        canvas.setColor(184, 132, 88, 128);
        FoodStats foodStats = player.getFoodStats();
        canvas.drawRoundedRect(0, -3, foodStats.getFoodLevel() * 7, 9, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, -4, 141, 10, 7);

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

    private static final List<ITextProperties> sTempTexts = new ArrayList<>();

    // config value
    public static boolean sTooltip;

    public static boolean drawTooltip(Canvas canvas, List<? extends ITextProperties> texts, ModernFontRenderer font,
                                      MatrixStack matrix, float mouseX, float mouseY, float width, float height) {
        if (!sTooltip)
            return false;

        // space between mouse and tooltip: 12
        // horizontal border thickness: 5
        // vertical border thickness: 4
        float tooltipX = mouseX + 12;
        float tooltipY = mouseY - 12;
        int tooltipWidth = 0;
        int tooltipHeight = 4 * 2;

        for (ITextProperties text : texts)
            tooltipWidth = Math.max(tooltipWidth, font.getStringPropertyWidth(text));

        boolean needWrap = false;
        if (tooltipX + tooltipWidth + 5 > width) {
            tooltipX = mouseX - 12 - 5 - tooltipWidth;
            if (tooltipX < 5) {
                tooltipWidth = (int) ((mouseX > width / 2) ? mouseX - 12 - 5 * 2 : width - 12 - 5 - mouseX);
                needWrap = true;
            }
        }

        int titleLinesCount = 1;
        if (needWrap) {
            int w = 0;
            final List<ITextProperties> l = sTempTexts;
            for (int i = 0; i < texts.size(); i++) {
                List<ITextProperties> wrapped = font.getCharacterManager().func_238362_b_(texts.get(i), tooltipWidth, Style.EMPTY);
                if (i == 0) titleLinesCount = wrapped.size();
                for (ITextProperties text : wrapped) {
                    w = Math.max(w, font.getStringPropertyWidth(text));
                    l.add(text);
                }
            }
            tooltipWidth = w;
            texts = l;
            tooltipX = (mouseX > width / 2) ? mouseX - 12 - 5 - tooltipWidth : mouseX + 12;
        }

        if (texts.size() > 1) {
            tooltipHeight += (texts.size() - 1) * 10; // line height
            if (texts.size() > titleLinesCount) tooltipHeight += 2; // gap
        }

        if (tooltipY < 4) tooltipY = 4;
        else if (tooltipY + tooltipHeight + 4 > height)
            tooltipY = height - tooltipHeight - 4;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 400);

        canvas.setColor(0, 0, 0, 208);
        canvas.drawRoundedRect(tooltipX - 5, tooltipY - 4, tooltipX + tooltipWidth + 5, tooltipY + tooltipHeight + 4, 3);
        canvas.setColor(170, 220, 240, 240);
        canvas.drawRoundedFrame(tooltipX - 5, tooltipY - 4, tooltipX + tooltipWidth + 5, tooltipY + tooltipHeight + 4, 3);

        final Matrix4f mat = matrix.getLast().getMatrix();
        final IRenderTypeBuffer.Impl buf = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        for (int i = 0; i < texts.size(); i++) {
            ITextProperties text = texts.get(i);
            if (text != null)
                font.drawText(text, tooltipX, tooltipY, 0xffffffff, true, mat, buf, false, 0, 0xf000f0);
            if (i + 1 == titleLinesCount) tooltipY += 2; // gap
            tooltipY += 10; // line height
        }
        buf.finish();

        GL11.glPopMatrix();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        sTempTexts.clear();
        return true;
    }
}
