package icyllis.modern.ui.test;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;

public class TestContainer extends Container {

    public TestContainer(int windowId, PlayerInventory inv) {
        super(null, windowId);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }
}
