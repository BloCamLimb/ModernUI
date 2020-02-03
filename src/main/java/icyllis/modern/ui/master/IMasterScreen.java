package icyllis.modern.ui.master;

import net.minecraft.client.gui.IGuiEventListener;

/**
 * Internal interface, implemented by UniversalModernScreen(G)
 */
public interface IMasterScreen {

    void addChild(IGuiEventListener eventListener);
}
