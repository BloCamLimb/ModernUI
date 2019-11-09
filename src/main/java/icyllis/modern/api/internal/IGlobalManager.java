package icyllis.modern.api.internal;

import icyllis.modern.api.module.IModuleInjector;
import net.minecraft.client.gui.FontRenderer;

public interface IGlobalManager extends IModuleInjector {

    void draw();

    void build(FontRenderer fontRenderer, int width, int height);

    void resize(int width, int height);

    void clear();

}
