package icyllis.modern.network;

import icyllis.modern.api.internal.IContainerProvider;
import icyllis.modern.api.internal.INetworkHandler;
import icyllis.modern.core.ModernUI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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
        openGUI(serverPlayer, containerProvider, null);
    }

    public void openGUI(ServerPlayerEntity serverPlayer, IContainerProvider containerProvider, BlockPos blockPos) {
        if(serverPlayer.world.isRemote) {
            return;
        }
        if(serverPlayer.container != serverPlayer.openContainer) {
            serverPlayer.closeContainer();
        }
        serverPlayer.getNextWindowId();
        int windowId = serverPlayer.currentWindowId;
        Container c = containerProvider.createContainer(windowId, serverPlayer.inventory, serverPlayer);
        if(c != null) {
            OpenContainer msg = new OpenContainer(containerProvider.getScreenType().getId(), windowId, blockPos != null, blockPos);
            sendTo(msg, serverPlayer);
            serverPlayer.openContainer = c;
            serverPlayer.openContainer.addListener(serverPlayer);
            MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(serverPlayer, c));
        }
    }
}
