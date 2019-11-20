package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.api.internal.IModuleReceiver;
import icyllis.modern.core.ModernUI;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class TestScreen implements IModernScreen {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    @Override
    public void createModules(IModuleReceiver receiver) {
        receiver.receiveModule(new TestMainModule()).setMain();
    }

    @Override
    public void updateFromNetwork(PacketBuffer extraData) {
        //ModernUI.logger.info(extraData.readBlockPos());
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
