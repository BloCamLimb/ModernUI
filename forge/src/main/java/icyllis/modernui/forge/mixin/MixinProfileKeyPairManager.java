/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge.mixin;

import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import icyllis.modernui.forge.ModernUIForge;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.util.CryptException;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ProfileKeyPairManager.class)
public class MixinProfileKeyPairManager {

    @Inject(method = "signer", at = @At("HEAD"), cancellable = true)
    private void onSigner(CallbackInfoReturnable<Optional<Signer>> info) {
        if (ModernUIForge.sSecureProfilePublicKey) {
            info.setReturnValue(null);
        }
    }

    @Inject(method = "parsePublicKey", at = @At("HEAD"))
    private static void onParsePublicKey(CallbackInfoReturnable<Optional<ProfilePublicKey.Data>> info) throws CryptException {
        if (ModernUIForge.sSecureProfilePublicKey) {
            throw new CryptException(new InsecurePublicKeyException.MissingException());
        }
    }

    @Inject(method = "profilePublicKey", at = @At("HEAD"), cancellable = true)
    private void onProfilePublicKey(CallbackInfoReturnable<Optional<ProfilePublicKey>> info) {
        if (ModernUIForge.sSecureProfilePublicKey) {
            info.setReturnValue(Optional.empty());
        }
    }
}
