package icyllis.modern.api.module;

import icyllis.modern.api.internal.IModuleReceiver;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Implements this to create your modern screen
 */
public interface IModernGui {

    /**
     * Inject your custom modules in Modern UI
     * If there's a module injected, make sure at least one is main
     *
     * @param receiver ModernUI module receiver
     */
    void createModules(IModuleReceiver receiver);

    /**
     * Called before create modules
     *
     * @param extraData A copy from container extra data
     */
    void updateData(PacketBuffer extraData);

    ITextComponent getTitle();
}
