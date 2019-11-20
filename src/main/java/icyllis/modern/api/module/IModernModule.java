package icyllis.modern.api.module;

import icyllis.modern.api.internal.IElementBuilder;

/**
 * A Module includes all elements also their position, texture, animation etc
 * You can consider this as a full part or a tab. That is, in the general case
 * there's only one module showing on your screen. But a screen can integrated
 * with more than one module and you can switch between each other via settings
 * in {@link IModuleTracker} also their triggers.
 */
public interface IModernModule {

    /**
     * Add your elements and set their properties
     *
     * @param builder Element builder
     */
    void createElements(IElementBuilder builder);

}
