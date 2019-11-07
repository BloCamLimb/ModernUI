package icyllis.modernui.client.test;

import icyllis.modernui.api.module.ICoordinateProvider;
import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IElementProvider;

public class TestMainModule implements IModernModule {

    public static final TestMainModule INSTANCE = new TestMainModule();

    @Override
    public void createElements(IElementProvider provider) {
        provider.newTextLine().setValue("2");
    }

    @Override
    public void setCoordinates(ICoordinateProvider provider) {
        provider.nextTextLine().setCoordinate(-20, -60);
    }
}
