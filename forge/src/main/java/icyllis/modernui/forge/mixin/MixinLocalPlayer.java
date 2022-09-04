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

import com.mojang.brigadier.ParseResults;
import icyllis.modernui.forge.ModernUIForge;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "signMessage", at = @At("HEAD"), cancellable = true)
    private void onSignMessage(MessageSigner signer,
                               ChatMessageContent content,
                               LastSeenMessages messages,
                               CallbackInfoReturnable<MessageSignature> ci) {
        if (ModernUIForge.sRemoveMessageSignature) {
            ci.setReturnValue(MessageSignature.EMPTY);
        }
    }

    @Inject(method = "signCommandArguments", at = @At("HEAD"), cancellable = true)
    private void onSignCommandArguments(MessageSigner signer,
                                        ParseResults<SharedSuggestionProvider> content,
                                        @Nullable Component decorated,
                                        LastSeenMessages messages,
                                        CallbackInfoReturnable<ArgumentSignatures> ci) {
        if (ModernUIForge.sRemoveMessageSignature) {
            ci.setReturnValue(ArgumentSignatures.EMPTY);
        }
    }
}
