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

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Internal use.
 */
@ApiStatus.Internal
public final class NetworkMessages extends NetworkHandler {

    private static final int S2C_OPEN_MENU = 0;

    static NetworkHandler sNetwork;

    NetworkMessages() {
        super(new ResourceLocation(ModernUIForge.ID, "network"), "360", true);
    }

    @SuppressWarnings("deprecation")
    static PacketBuffer openMenu(@Nonnull AbstractContainerMenu menu, @Nullable Consumer<FriendlyByteBuf> writer) {
        PacketBuffer buf = sNetwork.buffer(S2C_OPEN_MENU);
        buf.writeVarInt(menu.containerId);
        buf.writeVarInt(Registry.MENU.getId(menu.getType()));
        if (writer != null) {
            writer.accept(buf);
        }
        return buf;
    }
}
