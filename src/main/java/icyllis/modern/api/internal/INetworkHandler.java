package icyllis.modern.api.internal;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public interface INetworkHandler {

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
