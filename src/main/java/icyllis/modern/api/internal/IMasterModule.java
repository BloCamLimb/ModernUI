package icyllis.modern.api.internal;

import net.minecraft.client.gui.FontRenderer;

public interface IMasterModule {

    void draw();

    void build(FontRenderer fontRenderer, int width, int height);

    void resize(int width, int height);

    default boolean trigger(int id) {
        return false;
    }

    boolean triggered();
}
