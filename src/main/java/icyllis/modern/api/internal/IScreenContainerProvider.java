package icyllis.modern.api.internal;

import net.minecraft.inventory.container.INamedContainerProvider;

public interface IScreenContainerProvider extends INamedContainerProvider {

    IScreenType getScreenType();
}
