package icyllis.modernui.client.screen;

import icyllis.modernui.api.module.IModernScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModernScreen<T extends Screen & IModernScreen> extends Screen {

    private T screen;

    public ModernScreen(T screen) {
        super(screen.getTitle());
        this.screen = screen;
    }
}
