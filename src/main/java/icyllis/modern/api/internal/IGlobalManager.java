package icyllis.modern.api.internal;

import net.minecraft.client.gui.FontRenderer;

public interface IGlobalManager extends IModuleReceiver {

    void draw();

    void build(FontRenderer fontRenderer, int width, int height);

    void resize(int width, int height);

    void clear();

}
