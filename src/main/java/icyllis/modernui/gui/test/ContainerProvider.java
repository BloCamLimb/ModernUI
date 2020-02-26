package icyllis.modernui.gui.test;

import icyllis.modernui.api.global.IContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class ContainerProvider implements IContainerProvider {

    @Nonnull
    @Override
    public Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new ContainerTest(windowId, playerInventory, (TileEntity) null);
    }

    @Override
    public ResourceLocation getGui() {
        return UILibs.TEST_CONTAINER_SCREEN;
    }
}
