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

package icyllis.modernui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.screen.UIManager;
import icyllis.modernui.screen.ScrollController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractSelectionList.class)
public abstract class MixinSelectionList implements ScrollController.IListener {

    @Shadow
    public abstract int getMaxScroll();

    @Shadow
    public abstract double getScrollAmount();

    @Shadow
    private double scrollAmount;

    @Shadow
    @Final
    protected int itemHeight;

    @Shadow
    @Final
    public Minecraft minecraft;

    @Nullable
    private ScrollController mScrollController;

    /**
     * @author BloCamLimb
     * @reason Smooth scrolling
     */
    @Overwrite
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY != 0) {
            if (mScrollController != null) {
                mScrollController.setMaxScroll(getMaxScroll());
                mScrollController.scrollBy(Math.round(-scrollY * 40));
            } else {
                setScrollAmount(getScrollAmount() - scrollY * itemHeight / 2.0D);
            }
            return true;
        }
        return false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(PoseStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (mScrollController == null) {
            mScrollController = new ScrollController(this);
            skipAnimationTo(scrollAmount);
        }
        mScrollController.update(UIManager.getInstance().getElapsedTime());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderHeader(Lcom/mojang/blaze3d/vertex/PoseStack;IILcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void preRenderHeader(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderSystem.pushMatrix();
        RenderSystem.translated(0, ((int) (((int) getScrollAmount() - getScrollAmount()) * minecraft.getWindow().getGuiScale())) / minecraft.getWindow().getGuiScale(), 0);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderHeader(Lcom/mojang/blaze3d/vertex/PoseStack;IILcom/mojang/blaze3d/vertex/Tesselator;)V"))
    private void postRenderHeader(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderSystem.popMatrix();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderList(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIF)V"))
    private void preRenderList(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderSystem.pushMatrix();
        RenderSystem.translated(0, ((int) (((int) getScrollAmount() - getScrollAmount()) * minecraft.getWindow().getGuiScale())) / minecraft.getWindow().getGuiScale(), 0);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderList(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIF)V"))
    private void postRenderList(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        RenderSystem.popMatrix();
    }

    /**
     * @author BloCamLimb
     * @reason Smooth scrolling
     */
    @Overwrite
    public void setScrollAmount(double target) {
        if (mScrollController != null) {
            skipAnimationTo(target);
        } else
            scrollAmount = Mth.clamp(target, 0.0D, getMaxScroll());
    }

    @Override
    public void onScrollAmountUpdated(ScrollController controller, float amount) {
        scrollAmount = Mth.clamp(amount, 0.0D, getMaxScroll());
    }

    public void skipAnimationTo(double target) {
        assert mScrollController != null;
        mScrollController.setMaxScroll(getMaxScroll());
        mScrollController.scrollTo((float) target);
        mScrollController.abortAnimation();
    }
}
