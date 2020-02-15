package icyllis.modern.ui.test;

import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.system.ModernUI;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class ModuleTest {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "gui/gui_button.png");

    public void create(IElementBuilder builder) {
        builder.defaultBackground();
        builder.texture()
                .tex(BACKGROUND)
                .setRelPos(-128, -128)
                .uv(0, 0)
                .setSize(256, 256);
        builder.texture()
                .tex(FRAME)
                .setRelPos(-128, -128)
                .uv(0, 0)
                .setSize(256, 256)
                .setTint(0xeedc82);
        builder.text()
                .text(() -> TextFormatting.AQUA + "Please select a network")
                .setRelPos(0, -73)
                .align(0.25f);
        /*for (int i = 0; i < 7; i++) {
            int f = i;
            builder.navigation()
                    .setTexture(e -> e
                            .tex(BUTTON)
                            .uv(16 * f, 0)
                            .setRelPos(18 * f - 76, -99)
                            .setSize(16, 16)
                    )
                    .setRelPos(18 * f - 76, -99)
                    .setSize(16, 16)
                    .text(e -> e
                            .text(() -> "Network Connections")
                            .align(0.25f)
                            .setRelPos(18 * f - 68, -109)
                    )
                    .setTarget(i);
        }
        builder.navigation()
                .setTexture(e -> e
                        .tex(BUTTON)
                        .uv(112, 0)
                        .setRelPos(60, -99)
                        .setSize(16, 16)
                )
                .setRelPos(60, -99)
                .setSize(16, 16)
                .onMouseHoverOn(g -> g.getTexture().uv(112, 16))
                .text(e -> e
                        .text(() -> "Create New Network")
                        .align(0.25f)
                        .setRelPos(68, -109)
                )
                .setTarget(7);*/
        builder.text()
                .text(() -> "INFORMATION")
                .setRelPos(-68, -58)
                .scale(() -> 0.8f)
                .style();
        builder.text()
                .text(() -> "CONNECTION")
                .setRelPos(-68, 0)
                .scale(() -> 0.8f)
                .style();

    }

}
