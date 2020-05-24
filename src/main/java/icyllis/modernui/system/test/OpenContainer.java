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

package icyllis.modernui.system.test;

import icyllis.modernui.system.network.IMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

@Deprecated
public class OpenContainer {

    /*private ResourceLocation id;
    private int windowId;
    private PacketBuffer additionalData;

    public OpenContainer(ResourceLocation id, int windowId, PacketBuffer additionalData) {
        this.id = id;
        this.windowId = windowId;
        this.additionalData = additionalData;
    }

    @Override
    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeResourceLocation(id);
        buf.writeVarInt(windowId);
        buf.writeByteArray(additionalData.readByteArray());
    }

    @Override
    public void decode(@Nonnull PacketBuffer buf) {
        id = buf.readResourceLocation();
        windowId = buf.readVarInt();
        additionalData = new PacketBuffer(Unpooled.wrappedBuffer(buf.readByteArray(32600)));
    }

    @Override
    public boolean handle(NetworkEvent.Context ctx) {
        return false;
    }*/
}
