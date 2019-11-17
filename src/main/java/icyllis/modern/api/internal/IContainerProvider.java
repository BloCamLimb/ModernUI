package icyllis.modern.api.internal;

import icyllis.modern.api.ModernUITypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;

public interface IContainerProvider {

    Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity);

    ModernUITypes.Type getScreenType();
}
