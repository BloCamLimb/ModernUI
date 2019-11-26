package icyllis.modern.api.internal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;

public interface IContainerProvider {

    Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity);

    ResourceLocation getGui();
}
