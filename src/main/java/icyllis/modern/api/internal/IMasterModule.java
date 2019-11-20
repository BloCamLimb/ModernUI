package icyllis.modern.api.internal;

public interface IMasterModule {

    void draw();

    void build(int width, int height);

    void resize(int width, int height);

    default boolean trigger(int id) {
        return false;
    }

    boolean triggered();
}
