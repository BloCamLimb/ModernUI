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

package icyllis.modernui.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public interface IMessage {

    /**
     * Encode message to byte buffer
     *
     * @param buf buf to write
     */
    void encode(@Nonnull PacketBuffer buf);

    /**
     * Decode message from byte buffer
     *
     * @param buf buf to read
     */
    void decode(@Nonnull PacketBuffer buf);

    /**
     * Handle message on sided effective thread
     * To get the player {@link NetworkHandler#getPlayer(NetworkEvent.Context)}
     * To reply a message {@link NetworkHandler#reply(IMessage, NetworkEvent.Context)}
     * There's no need to call {@link NetworkEvent.Context#setPacketHandled(boolean)}
     *
     * @param ctx network context
     */
    void handle(@Nonnull NetworkEvent.Context ctx);
}
