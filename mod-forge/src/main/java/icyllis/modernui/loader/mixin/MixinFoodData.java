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

package icyllis.modernui.loader.mixin;

import icyllis.modernui.loader.forge.NetMessages;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class MixinFoodData {

    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;

    private float prevSaturationLevel;
    private float prevExhaustionLevel;

    private boolean needSync;

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(Player player, CallbackInfo ci) {
        if (saturationLevel != prevSaturationLevel || exhaustionLevel != prevExhaustionLevel) {
            prevSaturationLevel = saturationLevel;
            prevExhaustionLevel = exhaustionLevel;
            needSync = true;
        }
        if (needSync && (player.level.getGameTime() & 0x7) == 0) {
            NetMessages.food(saturationLevel, exhaustionLevel).sendToPlayer(player);
            needSync = false;
        }
    }
}
