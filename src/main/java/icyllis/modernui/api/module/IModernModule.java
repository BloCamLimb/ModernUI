package icyllis.modernui.api.module;

/**
 * A Module includes all elements also their position, texture, animation etc
 * You can consider this as a full gui or a gui tab. That is, in the general case
 * there's only one module showing on your screen. But a screen can integrated
 * with more than one module and you can switch between each other via settings
 * in {@link IModuleTracker} also their triggers
 *
 * You can make the module in singleton mode or new one when being injected
 */
public interface IModernModule {

    /**
     * Add your elements and set their properties
     *
     * @param provider A provider to create elements
     */
    void createElements(IElementProvider provider);

    /**
     * Set center coordinates for your elements or widgets
     *
     * @param provider Provider to set coordinates
     */
    void setCoordinates(ICoordinateProvider provider);
}
