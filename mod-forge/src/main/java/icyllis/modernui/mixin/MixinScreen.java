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

import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.screen.BlurHandler;
import icyllis.modernui.screen.TooltipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Mixin(Screen.class)
public class MixinScreen {

    //private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/block/spruce_planks.png");

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected ItemRenderer itemRenderer;

    @Shadow
    @Nullable
    protected Minecraft minecraft;

    @Redirect(
            method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient" +
                            "(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"
            )
    )
    private void renderBackgroundInWorld(@Nonnull Screen screen, @Nonnull PoseStack stack, int x1, int y1,
                                         int x2, int y2, int colorA, int colorB) {
        BlurHandler.INSTANCE.drawScreenBackground(screen, stack, x1, y1, x2, y2);
    }

    @Redirect(
            method = "renderTooltipInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z"
            ),
            remap = false
    )
    private boolean fakeEmpty(@Nonnull List<ClientTooltipComponent> list) {
        if (TooltipRenderer.sTooltip) {
            return true;
        }
        return list.isEmpty();
    }

    @Inject(method = "renderTooltipInternal", at = @At("TAIL"), remap = false)
    private void drawTooltip(PoseStack stack, List<ClientTooltipComponent> list, int x, int y, Font font,
                             CallbackInfo ci) {
        if (TooltipRenderer.sTooltip && !list.isEmpty()) {
            assert minecraft != null;
            TooltipRenderer.drawTooltip(stack, list, x, y, font, width, height, itemRenderer, minecraft);
        }
    }

    /*@Redirect(
            method = "renderDirtBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;bind
                    (Lnet/minecraft/resources/ResourceLocation;)V"
            )
    )
    private void bindDirtBackgroundTexture(@Nonnull TextureManager textureManager, ResourceLocation rl) {
        textureManager.bind(BACKGROUND);
    }*/
}
