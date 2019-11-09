package icyllis.modern.api.internal;

import icyllis.modern.api.module.IModuleInjector;
import net.minecraft.client.gui.FontRenderer;

public interface IModuleManager extends IModuleInjector {

    /**
     * Called after injecting modules
     */
    void build();

    void draw();

    void init(FontRenderer fontRenderer, int width, int height);

    void resize(int width, int height);

    void clear();

}
