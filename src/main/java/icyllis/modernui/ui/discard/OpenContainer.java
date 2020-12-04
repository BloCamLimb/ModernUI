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

package icyllis.modernui.ui.discard;

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
