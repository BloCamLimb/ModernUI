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
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.screen.Animation;
import icyllis.modernui.screen.CanvasForge;
import icyllis.modernui.textmc.ModernFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL20C.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;

public class TestHUD {

    public static boolean sBars;

    //private static DecimalFormat decimalFormat = new DecimalFormat("#.00");

    public static final TestHUD sInstance = new TestHUD();

    private Animation mBarAlphaAnim;
    private float mBarAlpha = 0.25f;

    private float mLastHealth;
    private int mLastHunger;
    private int mLastAir;

    {
        /*mBarAlphaAnim = new Animation(5000)
                .applyTo(new Applier(0.5f, 0.25f, () -> mBarAlpha, f -> mBarAlpha = f)
                        .setInterpolator(p -> Math.max((p - 0.8f) * 5.0f, 0)));*/
    }

    public void drawBars(@Nonnull CanvasForge canvas) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        Minecraft minecraft = Minecraft.getInstance();
        Window windowB3D = minecraft.getWindow();
        float aspectRatio = (float) windowB3D.getWidth() / windowB3D.getHeight();
        Matrix4.makePerspective(MathUtil.PI_DIV_2, aspectRatio, 1.0f, 100.0f)
                .get(sMat.rewind());
        GL11.glMultMatrixf(sMat.rewind());

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(-1.58f * aspectRatio, -1.0f, -1.8f);
        GL11.glScalef(1 / 90f, -1 / 90f, 1 / 90f);
        GL11.glRotatef(18, 0, 1, 0);

        // see ForgeIngameGUI
        Player player = (Player) minecraft.getCameraEntity();
        Objects.requireNonNull(player);

        float partialTicks = net.minecraftforge.client.model.animation.Animation.getPartialTickTime();

        float f = player.walkDist - player.walkDistO;
        float f1 = -(player.walkDist + f * partialTicks);
        float f2 = Mth.lerp(partialTicks, player.oBob, player.bob);
        GL11.glTranslatef(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5f,
                Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0f);
        GL11.glRotatef(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0f, 0, 0, 1);
        GL11.glRotatef(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2f) * f2) * 5.0f, 1, 0, 0);

        FoodData foodData = player.getFoodData();

        float health = player.getHealth();
        if (health != mLastHealth) {
            mLastHealth = health;
            mBarAlphaAnim.startFull();
        }
        float right = Math.min(health * 140 / player.getMaxHealth(), 140.0f);

        Paint paint = Paint.take();
        paint.reset();
        paint.setStrokeWidth(1.5f);
        paint.setAlpha((int) (mBarAlpha * 0xff));

        paint.setRGB(255, 19, 19);
        paint.setStyle(Paint.Style.FILL);
        //canvas.drawRoundRect(0, 25, right, 37, 6, paint);

        paint.setRGB(255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        //canvas.drawRoundRect(0, 25, 140, 37, 6, paint);

        int air = player.getAirSupply();
        if (air != mLastAir) {
            mLastAir = air;
            mBarAlphaAnim.startFull();
        }
        right = air * 140f / player.getMaxAirSupply();
        paint.setRGB(86, 184, 255);
        paint.setStyle(Paint.Style.FILL);
        //canvas.drawRoundRect(0, 11, right, 23, 6, paint);

        paint.setRGB(255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        //canvas.drawRoundRect(0, 11, 140, 23, 6, paint);

        int foodLevel = foodData.getFoodLevel();
        if (foodLevel != mLastHunger) {
            mLastHunger = foodLevel;
            mBarAlphaAnim.startFull();
        }
        right = foodLevel * 7;
        paint.setRGB(184, 132, 88);
        paint.setStyle(Paint.Style.FILL);
        //canvas.drawRoundRect(0, -3, right, 9, 6, paint);

        paint.setRGB(255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        //canvas.drawRoundRect(0, -3, 140, 9, 6, paint);

        /*canvas.resetColor();
        canvas.setAlpha((int) (mBarAlpha * 0xff * 2));
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.drawText(String.format("%.2f / %.2f", player.getHealth(), player.getMaxHealth()), 70, 27);
        canvas.drawText(String.format("%d / %d", player.getAirSupply(), player.getMaxAirSupply()), 70, 13);
        canvas.drawText(String.format("%d / %.2f / %.2f", foodData.getFoodLevel(), foodData.getSaturationLevel(),
                ((AccessFoodData) foodData).getExhaustionLevel()), 70, -1);*/

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

    private final List<FormattedText> mTempTexts = new ArrayList<>();

    // config value
    public static boolean sTooltip = true;
    public static int sTooltipR = 170;
    public static int sTooltipG = 220;
    public static int sTooltipB = 240;

    // space between mouse and tooltip
    private static final int TOOLTIP_SPACE = 12;
    private static final int H_BORDER = 4;
    private static final int V_BORDER = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int TITLE_GAP = 2;

    private static final FloatBuffer sMat = BufferUtils.createFloatBuffer(16);
    private static final Matrix4 myMat = new Matrix4();

    // test only, this can't handle complex paragraph layout
    public void drawTooltip(GLCanvas canvas, @Nonnull List<? extends FormattedText> texts, Font font, ItemStack stack,
                            PoseStack matrix, int eventX, int eventY, float mouseX, float mouseY,
                            float width, float height) {
        float tooltipX = mouseX + TOOLTIP_SPACE;
        float tooltipY = mouseY - TOOLTIP_SPACE;
        int tooltipWidth = 0;
        int tooltipHeight = V_BORDER * 2;

        for (FormattedText text : texts) {
            tooltipWidth = Math.max(tooltipWidth, font.width(text));
        }

        boolean needWrap = false;
        if (tooltipX + tooltipWidth + H_BORDER > width) {
            tooltipX = mouseX - TOOLTIP_SPACE - H_BORDER - tooltipWidth;
            if (tooltipX < H_BORDER) {
                tooltipWidth = (int) ((mouseX > width / 2) ? mouseX - TOOLTIP_SPACE - H_BORDER * 2 :
                        width - TOOLTIP_SPACE - H_BORDER - mouseX);
                needWrap = true;
            }
        }

        int titleLinesCount = 1;
        if (needWrap) {
            int w = 0;
            final List<FormattedText> temp = mTempTexts;
            for (int i = 0; i < texts.size(); i++) {
                List<FormattedText> wrapped = font.getSplitter().splitLines(texts.get(i), tooltipWidth, Style.EMPTY);
                if (i == 0)
                    titleLinesCount = wrapped.size();
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
            if (texts.size() > titleLinesCount)
                tooltipHeight += TITLE_GAP;
        }

        if (tooltipY < V_BORDER)
            tooltipY = V_BORDER;
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

        final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        Matrix4 projection = Matrix4.makeOrthographic(width, -height, 0, 2000);
        canvas.setProjection(projection);
        canvas.reset((int) width, (int) height);

        canvas.save();
        mat.store(sMat.rewind()); // Sodium check the remaining
        myMat.set(sMat.rewind());
        myMat.translate(0, 0, -2000);
        canvas.multiply(myMat);

        Paint paint = Paint.take();

        paint.setSmoothRadius(0.5f);

        paint.setRGBA(0, 0, 0, 208);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(tooltipX - H_BORDER, tooltipY - V_BORDER,
                tooltipX + tooltipWidth + H_BORDER, tooltipY + tooltipHeight + V_BORDER, 3, paint);

        paint.setRGBA(sTooltipR, sTooltipG, sTooltipB, 240);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tooltipX - H_BORDER, tooltipY - V_BORDER,
                tooltipX + tooltipWidth + H_BORDER, tooltipY + tooltipHeight + V_BORDER, 3, paint);
        /*canvas.drawRoundedFrameT1(tooltipX - H_BORDER, tooltipY - V_BORDER,
                tooltipX + tooltipWidth + H_BORDER, tooltipY + tooltipHeight + V_BORDER, 3);*/

        canvas.restore();
        canvas.draw();

        glBindVertexArray(oldVertexArray);
        glUseProgram(oldProgram);

        final MultiBufferSource.BufferSource source =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < texts.size(); i++) {
            FormattedText text = texts.get(i);
            if (text != null)
                ModernFontRenderer.drawText(text, tooltipX, tooltipY, 0xffffffff, true, mat, source,
                        false, 0, 0xf000f0);
            if (i + 1 == titleLinesCount)
                tooltipY += TITLE_GAP;
            tooltipY += LINE_HEIGHT;
        }
        source.endBatch();
        matrix.popPose();

        GL11.glPushMatrix();
        // because of the order of draw calls, we actually don't need z-shifting
        GL11.glTranslatef(partialX, partialY, 0);
        // compatibility with Forge mods, like Quark
        MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(stack, texts, matrix, tooltipLeft, tooltipTop,
                font, tooltipWidth, tooltipHeight));
        GL11.glPopMatrix();

        RenderSystem.enableDepthTest();
        mTempTexts.clear();
    }
}
