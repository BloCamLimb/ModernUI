package icyllis.modern.api.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public interface IContainerProvider {

    /**
     * Create a new container on server side
     * @return container
     */
    @Nonnull
    Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity);

    /**
     * Get gui registry name
     * @return gui registry name
     */
    ResourceLocation getGui();
}
