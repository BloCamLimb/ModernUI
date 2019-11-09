package icyllis.modern.api.tracker;

import icyllis.modern.api.basic.IEnd;

public interface IModuleTracker extends IEnd {

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
