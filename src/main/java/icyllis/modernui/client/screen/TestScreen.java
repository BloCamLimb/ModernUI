package icyllis.modernui.client.screen;

import icyllis.modernui.api.module.IModernScreen;
import icyllis.modernui.api.module.IModuleProvider;
import icyllis.modernui.client.module.TestMainModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class TestScreen extends Screen implements IModernScreen {

    protected TestScreen() {
        super(new StringTextComponent("test screen"));
    }

    @Override
    public void injectModules(IModuleProvider provider) {
        provider.injectModule(new TestMainModule());
    }
}
