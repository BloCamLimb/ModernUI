package icyllis.modern.ui.test;

import icyllis.modern.api.internal.IScreenContainerProvider;
import icyllis.modern.api.internal.IScreenType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;

public class ContainerHolder implements IScreenContainerProvider {

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("test container screen");
    }

    @Nullable
    @Override
    public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
        return new TestContainer(p_createMenu_1_, p_createMenu_2_);
    }

    @Override
    public IScreenType getScreenType() {
        return RegistryScreens.TEST_CONTAINER;
    }
}
