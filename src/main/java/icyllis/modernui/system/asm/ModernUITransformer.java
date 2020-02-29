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

package icyllis.modernui.system.asm;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import icyllis.modernui.system.ModernUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

@OnlyIn(Dist.CLIENT)
public class ModernUITransformer implements ITransformer<MethodNode> {

    private int index = 1;

    @Nonnull
    @Override
    public MethodNode transform(@Nonnull MethodNode methodNode, @Nonnull ITransformerVotingContext context) {
        if (index == 1) {
            ModernUI.LOGGER.debug("Transforming net.minecraft.client.Minecraft#displayInGameMenu");
            InsnList list = methodNode.instructions;
            Iterator<AbstractInsnNode> iterator = list.iterator();
            AbstractInsnNode inst;
            while (iterator.hasNext()) {
                inst = iterator.next();
                if (inst.getOpcode() == NEW) {
                    list.set(inst, new TypeInsnNode(NEW, "icyllis/modernui/impl/GuiIngameMenu"));
                }
                if (inst.getOpcode() == INVOKESPECIAL) {
                    list.set(inst, new MethodInsnNode(INVOKESPECIAL, "icyllis/modernui/impl/GuiIngameMenu", "<init>", "(Z)V", false));
                }
            }
        } else {
            InsnList list = methodNode.instructions;
            Iterator<AbstractInsnNode> iterator = list.iterator();
            AbstractInsnNode inst;
            boolean finish = false;
            while (iterator.hasNext()) {
                inst = iterator.next();
                if (finish) {
                    list.remove(inst);
                } else if (inst.getType() == LINE) {
                    InsnList cast = ASMAPI.listOf(
                            new VarInsnNode(ILOAD, 1),
                            new VarInsnNode(ALOAD, 0),
                            new FieldInsnNode(GETFIELD, "net/minecraft/client/MainWindow", "framebufferWidth", "I"),
                            new VarInsnNode(ALOAD, 0),
                            new FieldInsnNode(GETFIELD, "net/minecraft/client/MainWindow", "framebufferHeight", "I"),
                            new MethodInsnNode(INVOKESTATIC, "icyllis/modernui/system/asm/RewrittenMethods", "calcGuiScale", "(III)I", false),
                            new InsnNode(IRETURN));
                    list.insert(inst, cast);
                    finish = true;
                }
            }
        }
        index++;
        return methodNode;
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
        set.add(Target.targetMethod("net.minecraft.client.MainWindow", "calcGuiScale", "(IZ)I"));
        return set;
    }
}
