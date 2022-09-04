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

package icyllis.modernui.textmc.mixin;

import icyllis.modernui.textmc.ModernUITextMC;
import icyllis.modernui.textmc.TextLayoutEngine;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;

/**
 * Transform emoji shortcodes.
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Shadow
    protected EditBox input;

    private boolean mBroadcasting;

    // vanilla widget is REALLY buggy
    @Inject(method = "onEdited", at = @At("HEAD"))
    private void S_onEdited(String s, CallbackInfo ci) {
        String msg = input.getValue();
        if (!mBroadcasting &&
                !msg.startsWith("/") &&
                ModernUITextMC.CONFIG.mEmojiShortcodes.get() &&
                msg.contains(":")) {
            final TextLayoutEngine engine = TextLayoutEngine.getInstance();
            final Matcher matcher = TextLayoutEngine.EMOJI_SHORTCODE_PATTERN.matcher(msg);

            StringBuilder builder = null;
            int lastEnd = 0;
            boolean replaced = false;
            while (matcher.find()) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                int st = matcher.start();
                int en = matcher.end();
                String emojiSequence = null;
                if (en - st > 2) {
                    emojiSequence = engine.lookupEmojiShortcode(msg.substring(st + 1, en - 1));
                }
                if (emojiSequence != null) {
                    builder.append(msg, lastEnd, st);
                    builder.append(emojiSequence);
                    replaced = true;
                } else {
                    builder.append(msg, lastEnd, en);
                }
                lastEnd = en;
            }
            if (replaced) {
                builder.append(msg, lastEnd, msg.length());
                mBroadcasting = true;
                input.setValue(builder.toString());
                mBroadcasting = false;
            }
        }
    }
}
