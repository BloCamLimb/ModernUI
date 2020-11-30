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

package icyllis.modernui.system.mixin;

import icyllis.modernui.system.MsgEncoder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodStats.class)
public class MixinFoodStats {

    @Shadow
    private float foodSaturationLevel;
    @Shadow
    private float foodExhaustionLevel;

    private float prevSaturationLevel;
    private float prevExhaustionLevel;

    private boolean needSync;

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(PlayerEntity player, CallbackInfo ci) {
        if (foodSaturationLevel != prevSaturationLevel || foodExhaustionLevel != prevExhaustionLevel) {
            prevSaturationLevel = foodSaturationLevel;
            prevExhaustionLevel = foodExhaustionLevel;
            needSync = true;
        }
        if (needSync && (player.world.getGameTime() & 0x7) == 0) {
            MsgEncoder.food(foodSaturationLevel, foodExhaustionLevel).sendToPlayer(player);
            needSync = false;
        }
    }
}
