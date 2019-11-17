package icyllis.modern.api.internal;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface INetworkHandler {

    /**
     * Open a container both on server and client side and open a screen on client side.
     *
     * @param serverPlayer Player on server side
     * @param containerProvider Container provider
     */
    void openGUI(ServerPlayerEntity serverPlayer, IScreenContainerProvider containerProvider);

    /**
     * Open a container both on server and client side and open a screen on client side.
     *
     * @param serverPlayer Player on server side
     * @param containerProvider Container provider
     * @param blockPos Block pos to get TileEntity
     */
    void openGUI(ServerPlayerEntity serverPlayer, IScreenContainerProvider containerProvider, BlockPos blockPos);
}
