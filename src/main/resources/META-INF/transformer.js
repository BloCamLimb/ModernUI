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

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return wrapMethodTransformers({
        'replaceDisplayInGameMenu': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.Minecraft',
                'methodName': 'func_71385_j', // displayInGameMenu
                'methodDesc': '(Z)V'
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
                            new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/ModernUI", "displayInGameMenu", "(Z)V", false),
                            new InsnNode(Opcodes.RETURN));
                        list.insert(inst, cast);
                        finish = true;
                    }
                }
                return methodNode;
            }
        },
        'replaceGuiScaleAlgorithm': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.MainWindow',
                'methodName': 'func_216521_a', // calcGuiScale
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
                            /*new VarInsnNode(Opcodes.ALOAD, 0),
                            new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/MainWindow", "field_198131_r", "I"), // framebufferWidth
                            new VarInsnNode(Opcodes.ALOAD, 0),
                            new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/MainWindow", "field_198132_s", "I"), // framebufferHeight*/
                            new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/ModernUI", "calcGuiScale", "(I)I", false),
                            new InsnNode(Opcodes.IRETURN));
                        list.insert(inst, cast);
                        finish = true;
                    }
                }
                return methodNode;
            }
        },
        'replaceRenderBackgroundColor': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.screen.Screen',
                'methodName': 'renderBackground',
                'methodDesc': '(I)V'
            },
            'transformer': function (methodNode) {
                var list = methodNode.instructions;
                var invoke = ASMAPI.findFirstMethodCall(methodNode, ASMAPI.MethodType.VIRTUAL, "net/minecraft/client/gui/screen/Screen", "fillGradient", "(IIIIII)V")
                var ldc1 = invoke.getPrevious();
                var ldc2 = ldc1.getPrevious();
                list.remove(ldc1);
                list.remove(ldc2);
                var cast = ASMAPI.listOf(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/ModernUI", "getScreenBackgroundColor", "()I", false),
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/ModernUI", "getScreenBackgroundColor", "()I", false)
                );
                list.insertBefore(invoke, cast);
                return methodNode;
            }
        }/*,
        'replaceScreenPauseGame': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.screen.Screen',
                'methodName': 'isPauseScreen',
                'methodDesc': '()Z'
            },
            'transformer': function (methodNode) {
                var list = methodNode.instructions;
                var iterator = list.iterator();
                while(iterator.hasNext()) {
                    var inst = iterator.next();
                    if (inst.getType() === AbstractInsnNode.INSN) {
                        list.set(inst, new MethodInsnNode(Opcodes.INVOKESTATIC, "icyllis/modernui/system/CoreMethods", "isPauseScreen", "()Z", false));
                        break;
                    }
                }
                return methodNode;
            }
        }*/
    })
}

/**
 * Utility function to wrap all method transformers in class transformers
 * to make them run after OptiFine's class transformers
 *
 * @param {object} transformersObj All the transformers of this coremod.
 * @return {object} The transformersObj with all method transformers wrapped.
 */
function wrapMethodTransformers(transformersObj) {

    for (var transformerObjName in transformersObj) {
        var transformerObj = transformersObj[transformerObjName];

        var target = transformerObj["target"];
        if (!target)
            continue;

        var type = target["type"];
        if (!type || !type.equals("METHOD"))
            continue;

        var clazz = target["class"];
        if (!clazz)
            continue;

        var methodName = target["methodName"];
        if (!methodName)
            continue;

        var mappedMethodName = ASMAPI.mapMethod(methodName);

        var methodDesc = target["methodDesc"];
        if (!methodDesc)
            continue;

        var methodTransformer = transformerObj["transformer"];
        if (!methodTransformer)
            continue;

        var newTransformerObjName = "(Method2ClassTransformerWrapper) " + transformerObjName;
        transformersObj[newTransformerObjName] = {
            "target": {
                "type": "CLASS",
                "name": clazz,
            },
            "transformer": makeClass2MethodTransformerFunction(mappedMethodName, methodDesc, methodTransformer)
        };
        delete transformersObj[transformerObjName];
    }
    return transformersObj;
}

/**
 * Utility function for making the wrapper class transformer function
 * Not part of {@link #wrapMethodTransformers) because of scoping issues (Nashhorn
 * doesn't support "let" which would fix the issues)
 *
 * @param {string} mappedMethodName The (mapped) name of the target method
 * @param {string} methodDesc The description of the target method
 * @param {methodTransformer} methodTransformer The method transformer function
 * @return {function} A class transformer that wraps the methodTransformer
 */
function makeClass2MethodTransformerFunction(mappedMethodName, methodDesc, methodTransformer) {
    return function(classNode) {
        var methods = classNode.methods;
        for (var i in methods) {
            var methodNode = methods[i];
            if (!methodNode.name.equals(mappedMethodName))
                continue;
            if (!methodNode.desc.equals(methodDesc))
                continue;
            methods[i] = methodTransformer(methodNode);
            break;
        }
        return classNode;
    };
}
