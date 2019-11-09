package icyllis.modern.api.module;

public interface IModuleTracker {

    /**
     * Trigger this module when first opening
     * @return this
     */
    default IModuleTracker setMain() {
        setTrigger(-1);
        return this;
    }

    /**
     * Set trigger for the module
     * @param id trigger id
     * @return this
     */
    IModuleTracker setTrigger(int id);
}
