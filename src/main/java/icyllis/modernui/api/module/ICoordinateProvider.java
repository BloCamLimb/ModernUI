package icyllis.modernui.api.module;

import icyllis.modernui.api.internal.ICoordinate;

public interface ICoordinateProvider {

    default ICoordinate nextTextLine() {
        return nextTextLine(1);
    }

    ICoordinate nextTextLine(int count);
}
