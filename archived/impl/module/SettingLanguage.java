/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.module;

import com.google.common.collect.Lists;
import icyllis.modernui.test.discard.Module;
import icyllis.modernui.impl.setting.LanguageEntry;
import icyllis.modernui.impl.setting.LanguageGroup;
import icyllis.modernui.test.discard.ScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LanguageManager;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.resource.VanillaResourceType;

import javax.annotation.Nonnull;

public class SettingLanguage extends Module {

    private final Minecraft minecraft;

    private LanguageEntry highlight;

    public SettingLanguage() {
        this.minecraft = Minecraft.getInstance();
        ScrollWindow<LanguageGroup> window = new ScrollWindow<>(this, w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);

        LanguageGroup group = new LanguageGroup(this, window, minecraft.getLanguageManager());

        window.addGroups(Lists.newArrayList(group));

        addWidget(window);
    }

    @Override
    public boolean onBack() {
        applyLanguage();
        return false;
    }

    private void applyLanguage() {
        LanguageManager manager = minecraft.getLanguageManager();
        GameSettings gameSettings = minecraft.gameSettings;
        if (highlight != null && !highlight.getLanguage().getCode().equals(manager.getCurrentLanguage().getCode())) {
            manager.setCurrentLanguage(highlight.getLanguage());
            gameSettings.language = highlight.getLanguage().getCode();
            ForgeHooksClient.refreshResources(minecraft, VanillaResourceType.LANGUAGES);
            //FIXME
            //minecraft.fontRenderer.getBidiFlag(manager.isCurrentLanguageBidirectional());
            gameSettings.saveOptions();
        }
    }

    @Nonnull
    public LanguageEntry getHighlight() {
        return highlight;
    }

    public void setHighlight(@Nonnull LanguageEntry highlight) {
        this.highlight = highlight;
    }
}
