package icyllis.modern.ui.test;

import icyllis.modern.api.module.IGuiModule;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.system.ModernUI;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class ModuleTest implements IGuiModule {

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
                .size(256, 256)
                .alphaAnimation(a -> a.translate(-1f).fixedTiming(4f));
        builder.texture()
                .tex(FRAME)
                .pos(-128, -128)
                .uv(0, 0)
                .size(256, 256)
                .color(() -> 0xeedc82)
                .alphaAnimation(a -> a.translate(-1f).fixedTiming(4f));
        builder.textLine()
                .text(() -> TextFormatting.AQUA + "snownee likes to eat lemon")
                .pos(-50, -60);
        for (int i = 0; i < 7; i++) {
            int f = i;
            builder.navigation()
                    .tex(e -> e
                            .tex(BUTTON)
                            .uv(16 * f, 0)
                            .pos(18 * f - 76, -99)
                            .size(16, 16)
                            .alphaAnimation(a -> a
                                    .delay(f * 0.7f)
                                    .translate(-1f)
                                    .fixedTiming(2f)
                            )
                    )
                    .pos(18 * f - 76, -99)
                    .size(16, 16)
                    .text(e -> e
                            .text(() -> "\u6709\u8bf4")
                            .align(0.25f)
                            .pos(18 * f - 68, -109)
                    )
                    .to(i);
        }
        builder.navigation()
                .tex(e -> e.tex(BUTTON).uv(112, 0).pos(60, -99).size(16, 16)
                .alphaAnimation(a -> a.delay(5f).translate(-1f).fixedTiming(2f)))
                .pos(60, -99).size(16, 16)
                .text(e -> e.text(() -> "Create New Network").align(0.25f).pos(68, -109)).to(7);
        /*builder.textLine()
                .text(() -> "World partial ticks: " + df.format(Minecraft.getInstance().getRenderPartialTicks()))
                .pos(0, -20, true);*/

    }

}
