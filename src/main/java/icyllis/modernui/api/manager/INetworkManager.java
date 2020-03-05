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

package icyllis.modernui.api.manager;

import icyllis.modernui.api.global.IContainerProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public interface INetworkManager {

    /**
     * Open a container both on server and client side and open a screen on client side.
     *
     * @param serverPlayer Player on server side
     * @param containerProvider Container provider
     */
    void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider);

    /**
     * Open a container both on server and client side and open a screen on client side.
     *
     * @param serverPlayer Player on server side
     * @param containerProvider Container provider
     * @param blockPos Block pos
     */
    void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider, BlockPos blockPos);

    /**
     * Open a container both on server and client side and open a screen on client side.
     *
     * @param serverPlayer Player on server side
     * @param containerProvider Container provider
     * @param extraDataWriter Extra data
     */
    void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider, Consumer<PacketBuffer> extraDataWriter);
}
