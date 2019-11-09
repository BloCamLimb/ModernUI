package icyllis.modern.api.module;

public interface IPositionFixer {

    default IPositionFixer setNext(int x, int y) {
        return setNext(1, x, y);
    }

    IPositionFixer setNext(int count, int x, int y);
}
