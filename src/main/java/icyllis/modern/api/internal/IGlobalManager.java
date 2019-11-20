package icyllis.modern.api.internal;

public interface IGlobalManager extends IModuleReceiver {

    void draw();

    void build(int width, int height);

    void resize(int width, int height);

    void clear();

}
