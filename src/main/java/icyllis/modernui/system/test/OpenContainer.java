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

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

@Deprecated
public class OpenContainer {

    /*private final ResourceLocation id;
    private final int windowId;
    private final PacketBuffer additionalData;

    public OpenContainer(ResourceLocation id, int windowId, PacketBuffer additionalData) {
        this.id = id;
        this.windowId = windowId;
        this.additionalData = additionalData;
    }

    public static void encode(OpenContainer msg, PacketBuffer buf) {
        buf.writeResourceLocation(msg.id);
        buf.writeVarInt(msg.windowId);
        buf.writeByteArray(msg.additionalData.readByteArray());
    }

    public static OpenContainer decode(PacketBuffer buf) {
        return new OpenContainer(buf.readResourceLocation(), buf.readVarInt(), new PacketBuffer(Unpooled.wrappedBuffer(buf.readByteArray(32600))));
    }

    public static boolean handle(OpenContainer msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> GuiManager.INSTANCE.openContainerScreen(msg.id, msg.windowId, msg.additionalData));
        return true;
    }*/
}
