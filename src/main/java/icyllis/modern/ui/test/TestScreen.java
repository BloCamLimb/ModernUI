package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.api.module.IModuleInjector;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class TestScreen implements IModernScreen {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    @Override
    public void injectModules(IModuleInjector provider) {
        provider.injectModule(new TestMainModule())
                .setMain()
                .end();
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
