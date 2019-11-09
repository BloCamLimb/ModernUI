package icyllis.modern.core.network;

import icyllis.modern.api.ModernAPI;
import icyllis.modern.api.network.INetworkHandler;
import icyllis.modern.core.ModernUI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public enum NetworkHandler implements INetworkHandler {
    INSTANCE;

    private SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModernUI.MODID, "network"))
            .networkProtocolVersion(() -> "1")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();

    public void setup() {
        new ModernAPI(this);
    }

    public <M> void sendToServer(M message) {
        CHANNEL.sendToServer(message);
    }

    public <M> void sendTo(M message, ServerPlayerEntity playerMP) {
        CHANNEL.sendTo(message, playerMP.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }
}
