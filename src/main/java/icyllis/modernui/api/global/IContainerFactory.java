package icyllis.modernui.api.global;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Create container on client side
 * @param <T> Container class
 */
@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface IContainerFactory<T extends Container> {

    T create(int windowId, PlayerInventory playerInventory, PacketBuffer extraData);
}
