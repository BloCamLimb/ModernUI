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

package icyllis.modernui.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Internal use.
 */
public final class NetworkMessages {

    private static final int S2C_OPEN_MENU = 0;

    static NetworkHandler sNetwork;

    private NetworkMessages() {
    }

    static void openMenu(int containerId, int menuId, @Nullable Consumer<FriendlyByteBuf> writer, ServerPlayer p) {
        FriendlyByteBuf buf = NetworkHandler.buffer(S2C_OPEN_MENU);
        buf.writeVarInt(containerId);
        buf.writeVarInt(menuId);
        if (writer != null) {
            writer.accept(buf);
        }
        sNetwork.sendToPlayer(buf, p);
    }
}
