package icyllis.modernui.api.module;

/**
 * Implements this to create your own modules
 *
 * A Module includes all elements also their position, texture, animation etc
 * You can consider this as a full gui or a gui tab. That is, in the general case
 * there's only one module rendering on your screen. But a screen can integrated
 * with more than one module and switch between each other via settings in {@link IModuleTracker}
 *
 * You can make the module in singleton mode or new one when being injected
 */
public interface IModernModule {

    /**
     * Internal method, must set a field for tracker, and do not call this
     */
    void initTrack(IModuleTracker tracker);

    /**
     * Internal method, must return the tracker field, and do not call this
     */
    IModuleTracker getTracker();
}
