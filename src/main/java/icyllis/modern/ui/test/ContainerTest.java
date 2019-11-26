package icyllis.modern.ui.test;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;

public class ContainerTest extends Container {

    public ContainerTest(int windowId, PlayerInventory inv, PacketBuffer buf) {
        super(null, windowId);
        //ModernUI.logger.info(buf.readBlockPos());
    }

    public ContainerTest(int windowId, PlayerInventory inv, TileEntity tile) {
        super(null, windowId);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }
}
