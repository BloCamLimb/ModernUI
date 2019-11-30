/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modern.impl.chat;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CChatMessagePacket;

/**
 * Lengthened message
 */
public final class ChatMessage extends CChatMessagePacket {

    private String message;

    public ChatMessage() {
    }

    public ChatMessage(String message) {
        if(message.length() > 512) {
            message = message.substring(0, 512);
        }
        this.message = message;
    }

    @Override
    public void readPacketData(PacketBuffer buffer) {
        message = buffer.readString(512);
    }

    @Override
    public void writePacketData(PacketBuffer buffer) {
        buffer.writeString(message);
    }
}
