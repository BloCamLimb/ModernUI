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

package icyllis.modernui.system;

import icyllis.modernui.system.network.IMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public enum NetworkManager {
    INSTANCE;

    private final String PROTOCOL;

    private final SimpleChannel CHANNEL;

    private int index = 0;

    {
        PROTOCOL = ModList.get().getModFileById(ModernUI.MODID).getMods().get(0).getVersion().getQualifier();
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ModernUI.MODID, "main_network"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();
    }

    void registerMessages() {

    }

    /**
     * Register a network message
     *
     * @param type      message class
     * @param factory   supplier to create default new instance, should be a empty constructor
     * @param direction message direction, null for bi-directional message
     * @param <MSG>     message type
     */
    public <MSG extends IMessage> void registerMessage(@Nonnull Class<MSG> type, @Nonnull Supplier<MSG> factory, @Nullable NetworkDirection direction) {
        CHANNEL.messageBuilder(type, ++index, direction)
                .encoder(IMessage::encode)
                .decoder(buf -> decode(factory, buf))
                .consumer((BiConsumer<MSG, Supplier<NetworkEvent.Context>>) this::handle)
                .add();
    }

    @Nonnull
    private <MSG extends IMessage> MSG decode(@Nonnull Supplier<MSG> factory, PacketBuffer buf) {
        MSG msg = factory.get();
        msg.decode(buf);
        return msg;
    }

    private <MSG extends IMessage> void handle(@Nonnull MSG message, @Nonnull Supplier<NetworkEvent.Context> ctx) {
        IMessage.SimpleContext simple = new IMessage.SimpleContext(ctx.get());
        message.handle(simple);
        ctx.get().setPacketHandled(true);
    }

    public <MSG extends IMessage> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }

    public <MSG extends IMessage> void sendToPlayer(MSG message, @Nonnull PlayerEntity player) {
        try {
            sendToPlayer(message, (ServerPlayerEntity) player);
        } catch (ClassCastException ignored) {
            ModernUI.LOGGER.warn(ModernUI.MARKER, "Messages can't be sent to an illegal player");
        }
    }

    public <MSG extends IMessage> void sendToPlayer(MSG message, @Nonnull ServerPlayerEntity playerMP) {
        CHANNEL.sendTo(message, playerMP.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    public <MSG extends IMessage> void replay(MSG message, NetworkEvent.Context context) {
        CHANNEL.reply(message, context);
    }

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
