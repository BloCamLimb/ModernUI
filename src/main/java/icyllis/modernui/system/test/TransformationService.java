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

package icyllis.modernui.system.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cpw.mods.modlauncher.api.*;
import icyllis.modernui.system.ModernUI;
import net.minecraftforge.coremod.api.ASMAPI;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.objectweb.asm.Opcodes.*;

@Deprecated
public class TransformationService implements ITransformationService {

    /*private static Logger LOGGER = LogManager.getLogger();

    private static Marker MARKER = MarkerManager.getMarker("COREMOD");*/

    @Nonnull
    @Override
    public String name() {
        return ModernUI.MODID;
    }

    @Override
    public void initialize(@Nonnull IEnvironment environment) {
        // Transformation service is hard, only cpw knows
        throw new RuntimeException();
    }

    @Override
    public void beginScanning(@Nonnull IEnvironment environment) {

    }

    @Override
    public void onLoad(@Nonnull IEnvironment env, @Nonnull Set<String> otherServices) {

    }

    /*@Override
    public Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalClassesLocator() {
        Set<String> key = new HashSet<>();
        key.add("icyllis.modernui.");
        Supplier<Function<String, Optional<URL>>> value = () -> this::getResourceUrl;
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public Optional<URL> getResourceUrl(String name) {
        try {
            URL url = TransformationService.class.getProtectionDomain().getCodeSource().getLocation();
            URI uri = url.toURI();
            File file = new File(uri);
            ZipFile zipFile = new ZipFile(file);
            ZipEntry ze = zipFile.getEntry(name);
            if (ze == null)
                return Optional.empty();
            try {
                String ofZipUrlStr = url.toExternalForm();
                URL urlJar = new URL("jar:" + ofZipUrlStr + "!/" + name);
                return Optional.of(urlJar);
            } catch (IOException e) {
                return Optional.empty();
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }*/

    /*
    Fermion: Fuck you, ITransformer<?> is hard, wasn't it?
    Lex: Oh, fuck that project (fermion). */
    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Lists.newArrayList();
    }

    /*private static class Transformer implements ITransformer<MethodNode> {

        private int order = 0;

        @Nonnull
        @Override
        public MethodNode transform(@Nonnull MethodNode methodNode, @Nonnull ITransformerVotingContext context) {
            LOGGER.debug(MARKER, "Transforming {} with {} at order {}", methodNode.name, methodNode.desc, order);
            if (methodNode.name.equals("func_71385_j")) {
                InsnList list = methodNode.instructions;
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    AbstractInsnNode inst = list.get(i);
                    // New and InvokeSpecial will be called twice
                    if (inst.getOpcode() == NEW) {
                        list.set(inst, new TypeInsnNode(NEW, "icyllis/modernui/gui/screen/GuiIngameMenu"));
                    }
                    if (inst.getOpcode() == INVOKESPECIAL) {
                        list.set(inst, new MethodInsnNode(INVOKESPECIAL, "icyllis/modernui/gui/screen/GuiIngameMenu", "<init>", "(Z)V", false));
                    }
                }
            } else if (methodNode.name.equals("func_216521_a")) {
                InsnList list = methodNode.instructions;
                ListIterator<AbstractInsnNode> iterator = list.iterator();
                boolean finish = false;
                while (iterator.hasNext()) {
                    AbstractInsnNode inst = iterator.next();
                    if (finish) {
                        list.remove(inst);
                    } else if (inst.getType() == AbstractInsnNode.LINE) {
                        InsnList cast = ASMAPI.listOf(
                                new VarInsnNode(ILOAD, 1),
                                new MethodInsnNode(INVOKESTATIC, "icyllis/modernui/system/RewrittenMethods", "calcGuiScale", "(I)I", false),
                                new InsnNode(IRETURN));
                        list.insert(inst, cast);
                        finish = true;
                    }
                }
            }
            order++;
            return methodNode;
        }

        @Nonnull
        @Override
        public TransformerVoteResult castVote(@Nonnull ITransformerVotingContext context) {
            boolean optifineFinished = context.getAuditActivities().stream().anyMatch(activity ->
                    activity.getType() == ITransformerActivity.Type.TRANSFORMER && activity.getContext()[0].equals("OptiFine"));
            TransformerVoteResult result = (optifineFinished || !ModIntegration.optifineLoaded) ? TransformerVoteResult.YES : TransformerVoteResult.DEFER;
            return result;
        }

        @Nonnull
        @Override
        public Set<Target> targets() {
            return Sets.newHashSet(
                    //Target.targetMethod("net.minecraft.client.Minecraft", "displayInGameMenu", "(Z)V"),
                    Target.targetMethod("net.minecraft.client.Minecraft", remapName(INameMappingService.Domain.METHOD, "func_71385_j"), "(Z)V"),
                    //Target.targetMethod("net.minecraft.client.MainWindow", "calcGuiScale", "(IZ)I"),
                    Target.targetMethod("net.minecraft.client.MainWindow", remapName(INameMappingService.Domain.METHOD, "func_216521_a"), "(IZ)I")
                    );
        }

        public static String remapName(INameMappingService.Domain domain, String name) {
            return FMLLoader.getNameFunction("srg").map(f -> f.apply(domain, name)).orElse(name);
        }
    }*/
}
