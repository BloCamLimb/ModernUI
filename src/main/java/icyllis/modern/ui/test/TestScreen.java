package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.api.internal.IModuleReceiver;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class TestScreen implements IModernScreen {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    @Override
    public void createModules(IModuleReceiver receiver) {
        receiver.receiveModule(new TestMainModule()).setMain();
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
