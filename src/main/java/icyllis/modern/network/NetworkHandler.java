package icyllis.modern.network;

import icyllis.modern.api.internal.IContainerProvider;
import icyllis.modern.api.internal.INetworkHandler;
import icyllis.modern.core.ModernUI;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Consumer;

import static icyllis.modern.network.PlayMessages.*;

public enum NetworkHandler implements INetworkHandler {
    INSTANCE;

    private SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModernUI.MODID, "network"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();
    {
        registerMessages();
    }

    public void registerMessages() {
        int index = 0;
        CHANNEL.messageBuilder(OpenContainer.class, index).encoder(OpenContainer::encode).decoder(OpenContainer::decode).consumer(OpenContainer::handle).add();
    }

    public <M> void sendToServer(M message) {
        CHANNEL.sendToServer(message);
    }

    public <M> void sendTo(M message, ServerPlayerEntity playerMP) {
        CHANNEL.sendTo(message, playerMP.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
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

        if(c != null) {
            OpenContainer msg = new OpenContainer(containerProvider.getUIType().getId(), windowId, output);
            sendTo(msg, serverPlayer);
            serverPlayer.openContainer = c;
            serverPlayer.openContainer.addListener(serverPlayer);
            MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(serverPlayer, c));
        }
    }
}
