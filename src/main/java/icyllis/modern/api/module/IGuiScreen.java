package icyllis.modern.api.module;

import icyllis.modern.api.global.IModuleList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

/**
 * Implements this to create your modern gui screen
 */
public interface IGuiScreen {

    /**
     * Add your custom modules in Modern UI
     * ID is determined by order, and the first one is main
     *
     * @param list module list
     */
    void addModules(IModuleList list);

    ITextComponent getTitle();
}
