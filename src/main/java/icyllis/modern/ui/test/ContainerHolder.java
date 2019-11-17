package icyllis.modern.ui.test;

import icyllis.modern.api.ModernUITypes;
import icyllis.modern.api.internal.IContainerProvider;
import icyllis.modern.core.ModernUI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;

public class ContainerHolder implements IContainerProvider {

    @Override
    public Container createContainer(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new TestContainer(windowId, playerInventory, null);
    }

    @Override
    public ModernUITypes.Type getScreenType() {
        return RegistryScreens.TEST_CONTAINER_SCREEN;
    }
}
