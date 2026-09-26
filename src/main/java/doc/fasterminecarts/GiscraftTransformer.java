package doc.fasterminecarts;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class GiscraftTransformer implements IClassTransformer {
    private static final String TARGET_CHEST_CLASS = "net.minecraft.block.BlockChest";
    private static final String TARGET_CHEST_MODEL =
            "net.minecraft.client.model.ModelChest";
    private static final String TARGET_LARGE_CHEST_MODEL =
            "net.minecraft.client.model.ModelLargeChest";
    private static final String TARGET_CHEST_RENDERER =
            "net.minecraft.client.renderer.tileentity.TileEntityChestRenderer";
    private static final String TARGET_PIG_CONTROLLER =
            "net.minecraft.entity.ai.EntityAIControlledByPlayer";
    private static final String TARGET_PIG_CLASS =
            "net.minecraft.entity.passive.EntityPig";
    private static final String TARGET_OLD_LEAF_CLASS =
            "net.minecraft.block.BlockOldLeaf";
    private static final String TARGET_IC2_CLASS = "ic2.core.IC2";
    private static final String TARGET_PLAYER_CLASS =
            "net.minecraft.entity.player.EntityPlayer";
    private static final String CHEST_BOUNDS_DESC =
            "(Lnet/minecraft/world/IBlockAccess;III)V";
    private static final String LEAF_APPLE_DROP_DESC =
            "(Lnet/minecraft/world/World;IIIII)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return basicClass;
        }

        if (TARGET_CHEST_CLASS.equals(transformedName)) {
            return transformChestBounds(basicClass);
        }

        if (TARGET_CHEST_MODEL.equals(transformedName)
                || TARGET_LARGE_CHEST_MODEL.equals(transformedName)) {
            return transformChestModel(basicClass, transformedName);
        }

        if (TARGET_CHEST_RENDERER.equals(transformedName)) {
            return transformChestLidAngle(basicClass);
        }

        if (TARGET_PIG_CONTROLLER.equals(transformedName)) {
            return transformPigMovement(basicClass);
        }

        if (TARGET_PIG_CLASS.equals(transformedName)) {
            return transformPigSteeringItem(basicClass);
        }

        if (TARGET_OLD_LEAF_CLASS.equals(transformedName)) {
            return transformLeafAppleDrop(basicClass);
        }

        if (TARGET_IC2_CLASS.equals(transformedName) || TARGET_IC2_CLASS.equals(name)) {
            return transformRubberTreeRarity(basicClass);
        }

        if (TARGET_PLAYER_CLASS.equals(transformedName)) {
            return transformSuitMovementHunger(basicClass);
        }

        return basicClass;
    }

    private static byte[] transformChestBounds(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            boolean expectedName = "setBlockBoundsBasedOnState".equals(method.name)
                    || "func_149719_a".equals(method.name);

            if (!expectedName || !CHEST_BOUNDS_DESC.equals(method.desc)) {
                continue;
            }

            method.instructions.clear();
            method.tryCatchBlocks.clear();

            if (method.localVariables != null) {
                method.localVariables.clear();
            }

            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "doc/fasterminecarts/ChestShape",
                    "setFullCubeBounds",
                    "(Lnet/minecraft/block/Block;)V",
                    false));
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            patched = true;
            break;
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not patch BlockChest bounds.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformChestModel(
            byte[] basicClass,
            String transformedName) {

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            if (!"<init>".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (instruction.getOpcode() != Opcodes.RETURN) {
                    continue;
                }

                InsnList expansion = new InsnList();
                expansion.add(new VarInsnNode(Opcodes.ALOAD, 0));
                expansion.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "doc/fasterminecarts/ChestModelShape",
                        "expand",
                        "(Lnet/minecraft/client/model/ModelChest;)V",
                        false));
                method.instructions.insertBefore(instruction, expansion);
                patched = true;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not expand "
                            + transformedName + ".");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformChestLidAngle(byte[] basicClass) {
        final float vanillaPi = (float) Math.PI;
        final float limitedPi = (float) Math.toRadians(170.0D);

        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            boolean expectedName = "renderTileEntityAt".equals(method.name)
                    || "func_147500_a".equals(method.name);

            if (!expectedName) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (!(instruction instanceof LdcInsnNode)) {
                    continue;
                }

                Object constant = ((LdcInsnNode) instruction).cst;
                if (constant instanceof Float
                        && Float.compare((Float) constant, vanillaPi) == 0) {
                    method.instructions.set(
                            instruction,
                            new LdcInsnNode(limitedPi));
                    patched = true;
                    break;
                }
            }

            if (patched) {
                break;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not limit the chest lid angle.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformPigMovement(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            boolean expectedName = "updateTask".equals(method.name)
                    || "func_75246_d".equals(method.name);

            if (!expectedName || !"()V".equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }

                MethodInsnNode call = (MethodInsnNode) instruction;
                boolean movementCall = "moveEntityWithHeading".equals(call.name)
                        || "func_70612_e".equals(call.name);

                if (!movementCall || !"(FF)V".equals(call.desc)) {
                    continue;
                }

                method.instructions.set(
                        call,
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "doc/fasterminecarts/PigSpeedHandler",
                                "moveControlledPig",
                                "(Lnet/minecraft/entity/EntityLiving;FF)V",
                                false));
                patched = true;
                break;
            }

            break;
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not patch controlled pig movement.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformPigSteeringItem(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            boolean expectedName = "canBeSteered".equals(method.name)
                    || "func_82171_bF".equals(method.name);

            if (!expectedName || !"()Z".equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (instruction.getOpcode() != Opcodes.IRETURN) {
                    continue;
                }

                InsnList goldenItemCheck = new InsnList();
                goldenItemCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
                goldenItemCheck.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "doc/fasterminecarts/PigSpeedHandler",
                        "isHoldingGoldenControlItem",
                        "(Lnet/minecraft/entity/passive/EntityPig;)Z",
                        false));
                goldenItemCheck.add(new InsnNode(Opcodes.IOR));
                method.instructions.insertBefore(instruction, goldenItemCheck);
                patched = true;
            }

            break;
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not patch pig steering items.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformLeafAppleDrop(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            boolean expectedName = "func_150124_c".equals(method.name)
                    || "dropApple".equals(method.name);

            if (!expectedName || !LEAF_APPLE_DROP_DESC.equals(method.desc)) {
                continue;
            }

            method.instructions.clear();
            method.tryCatchBlocks.clear();

            if (method.localVariables != null) {
                method.localVariables.clear();
            }

            method.instructions.add(new InsnNode(Opcodes.RETURN));
            patched = true;
            break;
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not disable oak leaf apple drops.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformRubberTreeRarity(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            if (!"generate".equals(method.name) || method.desc == null
                    || !method.desc.contains("Ljava/util/Random;")) {
                continue;
            }

            AbstractInsnNode insn = method.instructions.getFirst();
            while (insn != null) {
                if (insn.getOpcode() == Opcodes.BIPUSH && insn instanceof IntInsnNode) {
                    IntInsnNode intInsn = (IntInsnNode) insn;
                    AbstractInsnNode next = insn.getNext();
                    if (intInsn.operand == 100
                            && next instanceof MethodInsnNode
                            && "nextInt".equals(((MethodInsnNode) next).name)
                            && "java/util/Random".equals(((MethodInsnNode) next).owner)) {
                        intInsn.setOpcode(Opcodes.SIPUSH);
                        intInsn.operand = 500;
                        patched = true;
                        break;
                    }
                }
                insn = insn.getNext();
            }

            if (patched) {
                break;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not reduce IC2 rubber tree rarity.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] transformSuitMovementHunger(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patchedMovement = false;
        boolean patchedJump = false;

        for (MethodNode method : classNode.methods) {
            if (("addMovementStat".equals(method.name) || "func_71000_j".equals(method.name))
                    && "(DDD)V".equals(method.desc)) {
                method.instructions.insert(newHungerSkip());
                patchedMovement = true;
            }

            if (("jump".equals(method.name) || "func_70664_aZ".equals(method.name))
                    && "()V".equals(method.desc)) {
                AbstractInsnNode insn = method.instructions.getFirst();
                while (insn != null) {
                    if (insn.getOpcode() == Opcodes.INVOKESPECIAL
                            && insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if ("jump".equals(call.name) || "func_70664_aZ".equals(call.name)) {
                            method.instructions.insert(insn, newHungerSkip());
                            patchedJump = true;
                            break;
                        }
                    }
                    insn = insn.getNext();
                }
            }
        }

        if (!patchedMovement || !patchedJump) {
            throw new RuntimeException(
                    "Giscraft could not disable suit movement hunger.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static InsnList newHungerSkip() {
        LabelNode continueLabel = new LabelNode();
        InsnList inject = new InsnList();
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "doc/fasterminecarts/SprintHandler",
                "wearingMovementSuit",
                "(Lnet/minecraft/entity/player/EntityPlayer;)Z",
                false));
        inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        inject.add(new InsnNode(Opcodes.RETURN));
        inject.add(continueLabel);
        return inject;
    }

}
