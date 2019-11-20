package icyllis.modern.ui.test;

import icyllis.modern.api.module.ModernUIType;
import icyllis.modern.api.internal.IContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntity;

public class ContainerProvider implements IContainerProvider {

    @Override
    public Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new TestContainer(windowId, playerInventory, (TileEntity) null);
    }

    @Override
    public ModernUIType getUIType() {
        return RegistryScreens.TEST_CONTAINER_SCREEN;
    }
}
