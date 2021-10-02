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

package icyllis.modernui.test.trash;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.screen.Animation;
import icyllis.modernui.screen.CanvasForge;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Objects;

@Deprecated
public class TestHUD {

    public static boolean sBars;

    //private static DecimalFormat decimalFormat = new DecimalFormat("#.00");

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

    private TestHUD() {
    }

    @Deprecated
    private void drawBars(@Nonnull CanvasForge canvas) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        //RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        Minecraft minecraft = Minecraft.getInstance();
        Window windowB3D = minecraft.getWindow();
        float aspectRatio = (float) windowB3D.getWidth() / windowB3D.getHeight();
        /*Matrix4.makePerspective(MathUtil.PI_DIV_2, aspectRatio, 1.0f, 100.0f)
                .get(mMatBuf.rewind());
        GL11.glMultMatrixf(mMatBuf.rewind());*/

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
        //RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();

        //minecraft.getTextureManager().bind(GuiComponent.GUI_ICONS_LOCATION);
    }
}
