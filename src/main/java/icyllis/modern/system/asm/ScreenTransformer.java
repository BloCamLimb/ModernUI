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

package icyllis.modern.system.asm;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

/**
 * Transform all new instance for vanilla screens
 */
public class ScreenTransformer implements ITransformer<MethodNode> {

    private static Logger LOGGER = LogManager.getLogger();

    @Nonnull
    @Override
    public MethodNode transform(@Nonnull MethodNode node, @Nonnull ITransformerVotingContext context) {
        LOGGER.debug("Transforming net.minecraft.client.Minecraft#displayInGameMenu");
        InsnList list = node.instructions;
        Iterator<AbstractInsnNode> iterator = list.iterator();
        AbstractInsnNode inst, cast;
        while (iterator.hasNext()) {
            inst = iterator.next();
            if (inst.getOpcode() == NEW) {
                cast = new TypeInsnNode(NEW, "icyllis/modern/impl/ingamemenu/GuiIngameMenu");
                list.set(inst, cast);
            }
            if (inst.getOpcode() == INVOKESPECIAL) {
                cast = new MethodInsnNode(INVOKESPECIAL, "icyllis/modern/impl/ingamemenu/GuiIngameMenu", "<init>", "(Z)V", false);
                list.set(inst, cast);
            }
        }
        return node;
    }

    @Nonnull
    @Override
    public TransformerVoteResult castVote(@Nonnull ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Nonnull
    @Override
    public Set<Target> targets() {
        Set<Target> set = new HashSet<>();
        set.add(Target.targetMethod("net.minecraft.client.Minecraft", "displayInGameMenu", "(Z)V"));
        return set;
    }
}
