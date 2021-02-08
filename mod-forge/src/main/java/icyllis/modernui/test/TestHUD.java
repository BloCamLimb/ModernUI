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

package icyllis.modernui.test;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import icyllis.modernui.graphics.text.ModernFontRenderer;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.mcimpl.mixin.AccessFoodData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestHUD {

    public static boolean sDing;
    public static boolean sFirstScreenOpened;

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
        Window mainWindow = minecraft.getWindow();
        float aspectRatio = (float) mainWindow.getWidth() / mainWindow.getHeight();
        RenderSystem.multMatrix(Matrix4f.perspective(90.0, aspectRatio, 1.0f, 100.0f));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(-1.58f * aspectRatio, -1.0f, -1.8f);
        GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
        GL11.glRotatef(12, 0, 1, 0);

        // see ForgeIngameGUI
        Player player = (Player) minecraft.getCameraEntity();
        Objects.requireNonNull(player);

        float partialTicks = Animation.getPartialTickTime();

        float f = player.walkDist - player.walkDistO;
        float f1 = -(player.walkDist + f * partialTicks);
        float f2 = Mth.lerp(partialTicks, player.oBob, player.bob);
        GL11.glTranslatef(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5f,
                Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0f);
        GL11.glRotatef(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0f, 0, 0, 1);
        GL11.glRotatef(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2f) * f2) * 5.0f, 1, 0, 0);

        canvas.setColor(255, 19, 19, 128);
        float r = Math.min(player.getHealth() * 140 / player.getMaxHealth(), 140);
        canvas.drawRoundedRect(0, 25, r, 37, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, 24, 141, 38, 7);
        canvas.setColor(86, 184, 255, 128);
        r = player.getAirSupply() * 140f / player.getMaxAirSupply();
        canvas.drawRoundedRect(0, 11, r, 23, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, 10, 141, 24, 7);
        canvas.setColor(184, 132, 88, 128);
        FoodData foodData = player.getFoodData();
        canvas.drawRoundedRect(0, -3, foodData.getFoodLevel() * 7, 9, 6);
        canvas.setColor(255, 255, 255, 128);
        canvas.drawRoundedFrame(-1, -4, 141, 10, 7);

        canvas.resetColor();
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(String.format("%.2f / %.2f", player.getHealth(), player.getMaxHealth()), 70, 27);
        canvas.drawText(String.format("%d / %d", player.getAirSupply(), player.getMaxAirSupply()), 70, 13);
        canvas.drawText(String.format("%d / %.2f / %.2f", foodData.getFoodLevel(), foodData.getSaturationLevel(),
                ((AccessFoodData) foodData).getExhaustionLevel()), 70, -1);

        RenderSystem.enableDepthTest();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();

        minecraft.getTextureManager().bind(GuiComponent.GUI_ICONS_LOCATION);
    }

    private static final List<FormattedText> sTempTexts = new ArrayList<>();

    // config value
    public static boolean sTooltip;
    public static int sTooltipR;
    public static int sTooltipG;
    public static int sTooltipB;

    // space between mouse and tooltip
    private static final int TOOLTIP_SPACE = 12;
    private static final int H_BORDER = 5;
    private static final int V_BORDER = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int TITLE_GAP = 2;

    // test only, this can't handle complex paragraph layout
    public static void drawTooltip(Canvas canvas, @Nonnull List<? extends FormattedText> texts,
                                   ModernFontRenderer font, ItemStack stack, PoseStack matrix,
                                   int eventX, int eventY, float mouseX, float mouseY, float width, float height) {
        float tooltipX = mouseX + TOOLTIP_SPACE;
        float tooltipY = mouseY - TOOLTIP_SPACE;
        int tooltipWidth = 0;
        int tooltipHeight = V_BORDER * 2;

        for (FormattedText text : texts)
            tooltipWidth = Math.max(tooltipWidth, font.width(text));

        boolean needWrap = false;
        if (tooltipX + tooltipWidth + H_BORDER > width) {
            tooltipX = mouseX - TOOLTIP_SPACE - H_BORDER - tooltipWidth;
            if (tooltipX < H_BORDER) {
                tooltipWidth = (int) ((mouseX > width / 2) ? mouseX - TOOLTIP_SPACE - H_BORDER * 2 : width - TOOLTIP_SPACE - H_BORDER - mouseX);
                needWrap = true;
            }
        }

        int titleLinesCount = 1;
        if (needWrap) {
            int w = 0;
            final List<FormattedText> temp = sTempTexts;
            for (int i = 0; i < texts.size(); i++) {
                List<FormattedText> wrapped = font.getSplitter().splitLines(texts.get(i), tooltipWidth, Style.EMPTY);
                if (i == 0) titleLinesCount = wrapped.size();
                for (FormattedText text : wrapped) {
                    w = Math.max(w, font.width(text));
                    temp.add(text);
                }
            }
            tooltipWidth = w;
            texts = temp;
            tooltipX = (mouseX > width / 2) ? mouseX - TOOLTIP_SPACE - H_BORDER - tooltipWidth : mouseX + TOOLTIP_SPACE;
        }

        if (texts.size() > 1) {
            tooltipHeight += (texts.size() - 1) * LINE_HEIGHT;
            if (texts.size() > titleLinesCount) tooltipHeight += TITLE_GAP;
        }

        if (tooltipY < V_BORDER) tooltipY = V_BORDER;
        else if (tooltipY + tooltipHeight + V_BORDER > height)
            tooltipY = height - tooltipHeight - V_BORDER;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        matrix.pushPose();
        matrix.translate(0, 0, 400); // because of the order of draw calls, we actually don't need z-shifting
        final Matrix4f mat = matrix.last().pose();

        // matrix transformation for x and y params, compatibility to MineColonies
        if (eventX != (int) mouseX || eventY != (int) mouseY) {
            // ignore partial pixels
            tooltipX += eventX - (int) mouseX;
            tooltipY += eventY - (int) mouseY;
        }

        // smoothing scaled pixels, keep the same partial value as mouse position since tooltipWidth and height are int
        final int tooltipLeft = (int) tooltipX;
        final int tooltipTop = (int) tooltipY;
        final float partialX = tooltipX - tooltipLeft;
        final float partialY = tooltipY - tooltipTop;

        GL11.glPushMatrix();
        RenderSystem.multMatrix(mat);

        canvas.setColor(0, 0, 0, 208);
        canvas.drawRoundedRect(tooltipX - H_BORDER, tooltipY - V_BORDER,
                tooltipX + tooltipWidth + H_BORDER, tooltipY + tooltipHeight + V_BORDER, 3);
        canvas.setColor(sTooltipR, sTooltipG, sTooltipB, 240);
        canvas.drawRoundedFrame(tooltipX - H_BORDER, tooltipY - V_BORDER,
                tooltipX + tooltipWidth + H_BORDER, tooltipY + tooltipHeight + V_BORDER, 3);

        GL11.glPopMatrix();

        final MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < texts.size(); i++) {
            FormattedText text = texts.get(i);
            if (text != null)
                font.drawText(text, tooltipX, tooltipY, 0xffffffff, true, mat, buf, false, 0, 0xf000f0);
            if (i + 1 == titleLinesCount) tooltipY += TITLE_GAP;
            tooltipY += LINE_HEIGHT;
        }
        buf.endBatch();
        matrix.popPose();

        GL11.glPushMatrix();
        GL11.glTranslatef(partialX, partialY, 0); // because of the order of draw calls, we actually don't need z-shifting
        // compatibility with Forge mods, like Quark
        MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(stack, texts, matrix, tooltipLeft, tooltipTop, font, tooltipWidth, tooltipHeight));
        GL11.glPopMatrix();

        RenderSystem.enableDepthTest();
        sTempTexts.clear();
    }
}
