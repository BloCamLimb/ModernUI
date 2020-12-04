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

import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

@Deprecated
public interface IGuiManager {

    /**
     * Register a gui with container on client side
     * @param id registry name
     * @param title screen title
     * @param containerFactory factory to create container
     * @param moduleFactory gui modules
     */
    <M extends Container> void registerContainerGui(ResourceLocation id, ITextComponent title, IContainerFactory<M> containerFactory, Consumer<IModuleFactory> moduleFactory);

    /*@Override
    public void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider) {
        openGUI(serverPlayer, containerProvider, buf -> {});
    }

    @Override
    public void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider, BlockPos blockPos) {
        openGUI(serverPlayer, containerProvider, buf -> buf.writeBlockPos(blockPos));
    }

    @Override
    public void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider, Consumer<PacketBuffer> extraDataWriter) {
        if(serverPlayer.world.isRemote) {
            return;
        }
        if(serverPlayer.container != serverPlayer.openContainer) {
            serverPlayer.closeContainer();
        }

        serverPlayer.getNextWindowId();
        int windowId = serverPlayer.currentWindowId;

        PacketBuffer extraData = new PacketBuffer(Unpooled.buffer());
        extraDataWriter.accept(extraData);
        extraData.readerIndex(0);

        PacketBuffer output = new PacketBuffer(Unpooled.buffer());
        output.writeVarInt(extraData.readableBytes());
        output.writeBytes(extraData);

        if (output.readableBytes() > 32600 || output.readableBytes() < 1) {
            throw new IllegalArgumentException("Invalid PacketBuffer for openGUI, found "+ output.readableBytes()+ " bytes");
        }

        Container c = containerProvider.createContainer(windowId, serverPlayer.inventory, serverPlayer);

        OpenContainer msg = new OpenContainer(containerProvider.getGui(), windowId, output);
        sendTo(msg, serverPlayer);
        serverPlayer.openContainer = c;
        serverPlayer.openContainer.addListener(serverPlayer);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(serverPlayer, c));
    }*/
}
