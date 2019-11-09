package icyllis.modern.api.basic;

public interface IValue<T, G> {

    G setValue(T value);

    T getValue();
}
