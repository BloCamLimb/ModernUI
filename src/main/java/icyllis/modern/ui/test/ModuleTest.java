package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.internal.IElementBuilder;
import icyllis.modern.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

import java.text.DecimalFormat;

public class ModuleTest implements IModernModule {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "gui/gui_button.png");

    @Override
    public void createElements(IElementBuilder builder) {
        //DecimalFormat df = new DecimalFormat("0.00");
        builder.defaultBackground();
        builder.texture()
                .tex(BACKGROUND)
                .pos(-128, -128)
                .uv(0, 0)
                .size(256, 256);
        builder.texture()
                .tex(FRAME)
                .pos(-128, -128)
                .uv(0, 0)
                .size(256, 256)
                .color(() -> 0xeedc82);
        builder.textLine()
                .text(() -> ":0210::0911: likes to eat :0311:")
                .pos(-50, -60);
        builder.textLine()
                .text(() -> "\u6709:000e:\u8bf4:090f:\uff0c\u786e\u5b9e")
                .pos(-50, -40);
        for (int i = 0; i < 7; i++) {
            int f = i;
            builder.navigation()
                    .tex(e -> e.tex(BUTTON).uv(16 * f, 0).pos(18 * f -76, -99).size(16, 16))
                    .pos(18 * f -76, -99).size(16, 16)
                    .text(e -> e.text(() -> "Home").align(0.25f).pos(18 * f -68, -109)).to(i);
        }
        builder.navigation()
                .tex(e -> e.tex(BUTTON).uv(112, 0).pos(60, -99).size(16, 16))
                .pos(60, -99).size(16, 16)
                .text(e -> e.text(() -> "Create New Network").align(0.25f).pos(68, -109)).to(7);
        /*builder.textLine()
                .text(() -> "World partial ticks: " + df.format(Minecraft.getInstance().getRenderPartialTicks()))
                .pos(0, -20, true);*/

    }

}
