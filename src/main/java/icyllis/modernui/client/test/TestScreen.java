package icyllis.modernui.client.test;

import icyllis.modernui.api.module.IModuleInjector;
import icyllis.modernui.api.module.IModuleProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class TestScreen implements IModuleInjector {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    @Override
    public void injectModules(IModuleProvider provider) {
        provider.injectModule(TestMainModule.INSTANCE)
                .setMain()
                .finish();
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
