package icyllis.modernui.api.module;

/**
 * Implements this for your screen to create Modern UI
 *
 * Must be implemented by a subclass of {@link net.minecraft.client.gui.screen.Screen}
 * or {@link net.minecraft.client.gui.screen.inventory.ContainerScreen}
 */
public interface IModernScreen {

    /**
     * Inject your custom modules in Modern UI
     * If there's a module injected, make sure at least one is main
     *
     * @param provider ModernUI module provider
     */
    void injectModules(IModuleProvider provider);
}
