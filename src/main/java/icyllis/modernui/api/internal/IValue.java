package icyllis.modernui.api.internal;

public interface IValue<T> {

    void setValue(T value);

    T getValue();
}
