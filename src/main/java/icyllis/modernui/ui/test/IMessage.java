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

package icyllis.modernui.ui.test;

import icyllis.modernui.network.NetworkHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

@Deprecated
public interface IMessage {

    /**
     * Encode this message to byte buffer
     *
     * @param buffer buffer to write
     */
    void encode(@Nonnull PacketBuffer buffer);

    /**
     * Decode this message from byte buffer and handle this message on sided effective thread.
     * <p>
     * To get the player {@link NetworkHandler#getPlayer(NetworkEvent.Context)}
     * To reply a message {@link NetworkHandler#reply(IMessage, NetworkEvent.Context)}
     * <p>
     * It is not allowed to call {@link NetworkEvent.Context#setPacketHandled(boolean)}
     * or {@link NetworkEvent.Context#enqueueWork(Runnable)}, they are redundant
     *
     * @param buffer  buffer to read
     * @param context network context
     */
    void handle(@Nonnull PacketBuffer buffer, @Nonnull NetworkEvent.Context context);
}
