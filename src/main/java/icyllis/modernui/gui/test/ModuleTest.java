/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.test;

import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.math.Locator;
import icyllis.modernui.gui.widget.NumberInputField;
import icyllis.modernui.gui.widget.SlidingToggleButton;
import icyllis.modernui.gui.widget.TextField;
import icyllis.modernui.system.ModernUI;
import net.minecraft.util.ResourceLocation;

public class ModuleTest extends Module {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_button.png");

    private final NumberInputField h;

    public ModuleTest() {
        addWidget(new SlidingToggleButton.Builder(0x8020a0e0, 0x40808080, 4)
                .setLocator(new Locator(-10, -60))
                .build(this));
        addWidget(h = new NumberInputField(this, new NumberInputField.Builder().setWidth(120).setHeight(12)));
        h.setLimit(-997, 600);
        h.setDecoration(f -> new TextField.Frame(f, "Limit:", -1));
        h.setNumberListener(e -> {}, true);
        addDrawable(new TestDraw());
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        h.locate(width / 2f - 60, height / 2f - 44);
    }

    public void create() {

        /*builder.texture()
                .setTexture(BACKGROUND)
                .setPos(-128, -128)
                .setUV(0, 0)
                .setSize(256, 256)
                .buildToPool();
        builder.texture()
                .setTexture(FRAME)
                .setPos(-128, -128)
                .setUV(0, 0)
                .setSize(256, 256)
                .setTint(0xeedc82)
                .buildToPool();
        builder.textLine()
                .text(() -> TextFormatting.AQUA + "Please select a network")
                .setPos(0, -73)
                .align(0.25f);*/
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

    }

}
