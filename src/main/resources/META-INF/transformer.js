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

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return {
        'replaceIngameMenu': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.Minecraft',
                'methodName': 'func_71385_j',
                'methodDesc': '(Z)V'
            },
            'transformer': function (methodNode) {
                var list = methodNode.instructions;
                var size = list.size();
                for (var i = 0; i < size; i++) {
                    var inst = list.get(i);
                    // New and InvokeSpecial will be called twice
                    if (inst.getOpcode() === Opcodes.NEW) {
                        list.set(inst, new TypeInsnNode(Opcodes.NEW, "icyllis/modernui/impl/GuiIngameMenu"));
                    }
                    if (inst.getOpcode() === Opcodes.INVOKESPECIAL) {
                        list.set(inst, new MethodInsnNode(Opcodes.INVOKESPECIAL, "icyllis/modernui/impl/GuiIngameMenu", "<init>", "(Z)V", false));
                    }
                }
                return methodNode;
            }
        },
        'replaceGuiScaleCalculation': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.MainWindow',
                'methodName': 'calcGuiScale',
                'methodDesc': '(IZ)I'
            },
            'transformer': function (methodNode) {
                var list = methodNode.instructions;
                var iterator = list.iterator();
                var finish = false;
                while(iterator.hasNext()) {
                    var inst = iterator.next();
                    if (finish) {
                        list.remove(inst);
                    } else if (inst.getType() === AbstractInsnNode.LINE) {
                        var cast = ASMAPI.listOf(
                            new VarInsnNode(Opcodes.ILOAD, 1),
                            new VarInsnNode(Opcodes.ALOAD, 0),
                            new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/MainWindow", "framebufferWidth", "I"),
                            new VarInsnNode(Opcodes.ALOAD, 0),
                            new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/MainWindow", "framebufferHeight", "I"),
                            new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/RewrittenMethods", "calcGuiScale", "(III)I", false),
                            new InsnNode(Opcodes.IRETURN));
                        list.insert(inst, cast);
                        finish = true;
                    }
                }
                return methodNode;
            }
        }
    }
}
