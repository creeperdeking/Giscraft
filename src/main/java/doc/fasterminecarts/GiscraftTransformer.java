package doc.fasterminecarts;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
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
    private static final String TARGET_PAUSE_MENU =
            "net.minecraft.client.gui.GuiIngameMenu";
    private static final String TARGET_HILLS_BIOME =
            "net.minecraft.world.biome.BiomeGenHills";
    private static final String TARGET_ANVIL_CONTAINER =
            "net.minecraft.inventory.ContainerRepair";
    private static final String TARGET_ANVIL_RESULT_SLOT =
            "net.minecraft.inventory.ContainerRepair$2";
    private static final String TARGET_SUGAR_CANE_GEN =
            "net.minecraft.world.gen.feature.WorldGenReed";
    private static final String TARGET_CACTUS_GEN =
            "net.minecraft.world.gen.feature.WorldGenCactus";
    private static final String TARGET_OLD_SUGAR_CANE_GEN = "owg.deco.OldGenReed";
    private static final String TARGET_OLD_CACTUS_GEN = "owg.deco.OldGenCactus";
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

        if (TARGET_PAUSE_MENU.equals(transformedName)) {
            return transformPauseMenuTitle(basicClass);
        }

        if (TARGET_HILLS_BIOME.equals(transformedName)) {
            return transformEmeraldGeneration(basicClass);
        }

        if (TARGET_ANVIL_CONTAINER.equals(transformedName)) {
            return transformAnvilContainer(basicClass);
        }

        if (TARGET_ANVIL_RESULT_SLOT.equals(transformedName)) {
            return transformAnvilResultSlot(basicClass);
        }

        if (isFullGrownPlant(name) || isFullGrownPlant(transformedName)) {
            return transformFullGrownPlant(basicClass, transformedName);
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

    private static boolean isFullGrownPlant(String className) {
        return TARGET_SUGAR_CANE_GEN.equals(className)
                || TARGET_CACTUS_GEN.equals(className)
                || TARGET_OLD_SUGAR_CANE_GEN.equals(className)
                || TARGET_OLD_CACTUS_GEN.equals(className);
    }

    private static byte[] transformFullGrownPlant(byte[] basicClass, String transformedName) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            if (replacePlantHeight(method)) {
                patched = true;
                break;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not fully grow " + transformedName + ".");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean replacePlantHeight(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (instruction.getOpcode() != Opcodes.ISTORE) {
                continue;
            }

            AbstractInsnNode[] earlier = new AbstractInsnNode[9];
            AbstractInsnNode cursor = instruction;
            boolean complete = true;
            for (int index = 0; index < earlier.length; index++) {
                cursor = previousReal(cursor);
                if (cursor == null) {
                    complete = false;
                    break;
                }
                earlier[index] = cursor;
            }
            if (!complete) {
                continue;
            }

            AbstractInsnNode secondAdd = earlier[0];
            AbstractInsnNode secondNextInt = earlier[1];
            AbstractInsnNode firstAdd = earlier[2];
            AbstractInsnNode plusOne = earlier[3];
            AbstractInsnNode firstNextInt = earlier[4];
            AbstractInsnNode bound = earlier[5];
            AbstractInsnNode randomInner = earlier[6];
            AbstractInsnNode randomOuter = earlier[7];
            AbstractInsnNode base = earlier[8];

            if (secondAdd == null || secondAdd.getOpcode() != Opcodes.IADD
                    || firstAdd == null || firstAdd.getOpcode() != Opcodes.IADD
                    || plusOne == null || plusOne.getOpcode() != Opcodes.ICONST_1
                    || bound == null || bound.getOpcode() != Opcodes.ICONST_3
                    || !isRandomNextInt(firstNextInt)
                    || !isRandomNextInt(secondNextInt)
                    || randomInner == null || randomInner.getOpcode() != Opcodes.ALOAD
                    || randomOuter == null || randomOuter.getOpcode() != Opcodes.ALOAD
                    || base == null
                    || (base.getOpcode() != Opcodes.ICONST_1
                            && base.getOpcode() != Opcodes.ICONST_2)) {
                continue;
            }

            if (!(instruction instanceof VarInsnNode)) {
                continue;
            }

            InsnList fullHeight = new InsnList();
            fullHeight.add(new InsnNode(Opcodes.ICONST_3));
            fullHeight.add(new VarInsnNode(
                    Opcodes.ISTORE,
                    ((VarInsnNode) instruction).var));
            method.instructions.insert(instruction, fullHeight);
            return true;
        }

        return false;
    }

    private static boolean isRandomNextInt(AbstractInsnNode instruction) {
        return instruction instanceof MethodInsnNode
                && instruction.getOpcode() == Opcodes.INVOKEVIRTUAL
                && "java/util/Random".equals(((MethodInsnNode) instruction).owner)
                && "nextInt".equals(((MethodInsnNode) instruction).name)
                && "(I)I".equals(((MethodInsnNode) instruction).desc);
    }

    private static byte[] transformEmeraldGeneration(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (instruction.getOpcode() != Opcodes.GETSTATIC
                        || !(instruction instanceof FieldInsnNode)
                        || !isEmeraldOreField((FieldInsnNode) instruction)) {
                    continue;
                }

                AbstractInsnNode z = previousReal(instruction);
                AbstractInsnNode y = previousReal(z);
                AbstractInsnNode x = previousReal(y);
                AbstractInsnNode world = previousReal(x);
                AbstractInsnNode meta = nextReal(instruction);
                AbstractInsnNode flags = nextReal(meta);
                AbstractInsnNode setBlock = nextReal(flags);
                AbstractInsnNode pop = nextReal(setBlock);

                if (world == null || world.getOpcode() != Opcodes.ALOAD
                        || x == null || x.getOpcode() != Opcodes.ILOAD
                        || y == null || y.getOpcode() != Opcodes.ILOAD
                        || z == null || z.getOpcode() != Opcodes.ILOAD
                        || meta == null || meta.getOpcode() != Opcodes.ICONST_0
                        || flags == null || flags.getOpcode() != Opcodes.ICONST_2
                        || setBlock == null
                        || setBlock.getOpcode() != Opcodes.INVOKEVIRTUAL
                        || pop == null || pop.getOpcode() != Opcodes.POP) {
                    continue;
                }

                method.instructions.remove(world);
                method.instructions.remove(x);
                method.instructions.remove(y);
                method.instructions.remove(z);
                method.instructions.remove(instruction);
                method.instructions.remove(meta);
                method.instructions.remove(flags);
                method.instructions.remove(setBlock);
                method.instructions.remove(pop);
                patched = true;
                break;
            }

            if (patched) {
                break;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not disable emerald world generation.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isEmeraldOreField(FieldInsnNode field) {
        return "emerald_ore".equals(field.name)
                || "field_150412_bA".equals(field.name)
                || "bA".equals(field.name);
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static byte[] transformAnvilContainer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean removedLimit = false;
        boolean removedCost = false;

        for (MethodNode method : classNode.methods) {
            if (removeTooExpensiveLimit(method)) {
                removedLimit = true;
                removedCost = zeroAnvilLevelCost(method);
                break;
            }
        }

        if (!removedLimit || !removedCost) {
            throw new RuntimeException(
                    "Giscraft could not remove anvil level costs.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean removeTooExpensiveLimit(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (instruction.getOpcode() != Opcodes.BIPUSH
                    || !(instruction instanceof IntInsnNode)
                    || ((IntInsnNode) instruction).operand != 40) {
                continue;
            }

            AbstractInsnNode compare = nextReal(instruction);
            if (compare == null
                    || compare.getOpcode() != Opcodes.IF_ICMPLT
                    || !(compare instanceof JumpInsnNode)) {
                continue;
            }

            LabelNode skip = ((JumpInsnNode) compare).label;
            AbstractInsnNode cursor = compare.getNext();
            AbstractInsnNode clearResult = null;
            AbstractInsnNode storeResult = null;
            while (cursor != null && cursor != skip) {
                if (cursor.getOpcode() == Opcodes.ACONST_NULL) {
                    AbstractInsnNode store = nextReal(cursor);
                    if (store != null && store.getOpcode() == Opcodes.ASTORE) {
                        clearResult = cursor;
                        storeResult = store;
                        break;
                    }
                }
                cursor = cursor.getNext();
            }

            if (clearResult == null) {
                continue;
            }

            AbstractInsnNode costField = previousReal(instruction);
            AbstractInsnNode loadContainer = previousReal(costField);
            if (loadContainer == null || costField == null
                    || costField.getOpcode() != Opcodes.GETFIELD) {
                continue;
            }

            AbstractInsnNode current = loadContainer;
            AbstractInsnNode end = storeResult.getNext();
            while (current != null && current != end) {
                AbstractInsnNode next = current.getNext();
                if (current.getOpcode() >= 0) {
                    method.instructions.remove(current);
                }
                current = next;
            }
            return true;
        }

        return false;
    }

    private static boolean zeroAnvilLevelCost(MethodNode method) {
        FieldInsnNode costField = null;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (instruction.getOpcode() == Opcodes.GETFIELD
                    && instruction instanceof FieldInsnNode
                    && nextReal(instruction) instanceof IntInsnNode
                    && nextReal(instruction).getOpcode() == Opcodes.BIPUSH
                    && ((IntInsnNode) nextReal(instruction)).operand == 40) {
                costField = (FieldInsnNode) instruction;
                break;
            }
        }

        if (costField == null) {
            return false;
        }

        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (!(instruction instanceof MethodInsnNode)
                    || instruction.getOpcode() != Opcodes.INVOKEVIRTUAL
                    || !"()V".equals(((MethodInsnNode) instruction).desc)) {
                continue;
            }

            if (nextReal(instruction) == null
                    || nextReal(instruction).getOpcode() != Opcodes.RETURN) {
                continue;
            }

            InsnList clearCost = new InsnList();
            clearCost.add(new VarInsnNode(Opcodes.ALOAD, 0));
            clearCost.add(new InsnNode(Opcodes.ICONST_0));
            clearCost.add(new FieldInsnNode(
                    Opcodes.PUTFIELD,
                    costField.owner,
                    costField.name,
                    costField.desc));
            method.instructions.insertBefore(instruction, clearCost);
            return true;
        }

        return false;
    }

    private static byte[] transformAnvilResultSlot(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patchedTake = false;
        boolean patchedPickup = false;

        for (MethodNode method : classNode.methods) {
            if (patchedTake || !returnsBoolean(method) || !containsOpcode(method, Opcodes.IF_ICMPLT)) {
                continue;
            }

            MethodInsnNode hasStack = findInvoke(method, Opcodes.INVOKEVIRTUAL, "()Z");
            if (hasStack == null) {
                continue;
            }

            String owner = hasStack.owner;
            String name = hasStack.name;
            String desc = hasStack.desc;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) {
                method.localVariables.clear();
            }
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    owner,
                    name,
                    desc,
                    false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            patchedTake = true;
        }

        for (MethodNode method : classNode.methods) {
            MethodInsnNode chargeLevels = findInvoke(method, Opcodes.INVOKEVIRTUAL, "(I)V");
            if (chargeLevels == null) {
                continue;
            }

            AbstractInsnNode current = method.instructions.getFirst();
            AbstractInsnNode end = chargeLevels.getNext();
            while (current != null && current != end) {
                AbstractInsnNode next = current.getNext();
                if (current.getOpcode() >= 0) {
                    method.instructions.remove(current);
                }
                current = next;
            }
            patchedPickup = true;
            break;
        }

        if (!patchedTake || !patchedPickup) {
            throw new RuntimeException(
                    "Giscraft could not remove anvil level costs.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean returnsBoolean(MethodNode method) {
        return method.desc != null && method.desc.endsWith(")Z");
    }

    private static boolean containsOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }

    private static MethodInsnNode findInvoke(MethodNode method, int opcode, String desc) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() == opcode
                    && instruction instanceof MethodInsnNode
                    && desc.equals(((MethodInsnNode) instruction).desc)) {
                return (MethodInsnNode) instruction;
            }
        }
        return null;
    }

    private static byte[] transformPauseMenuTitle(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            if (!"(IIF)V".equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {

                if (!(instruction instanceof MethodInsnNode)
                        || instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                    continue;
                }

                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.desc == null
                        || !call.desc.endsWith("Ljava/lang/String;III)V")) {
                    continue;
                }

                AbstractInsnNode color = previousReal(call);
                if (!(color instanceof LdcInsnNode)
                        || !Integer.valueOf(16777215).equals(((LdcInsnNode) color).cst)) {
                    continue;
                }

                AbstractInsnNode titleY = previousReal(color);
                if (!(titleY instanceof IntInsnNode)
                        || titleY.getOpcode() != Opcodes.BIPUSH
                        || ((IntInsnNode) titleY).operand != 40) {
                    continue;
                }

                method.instructions.set(
                        titleY,
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "doc/fasterminecarts/PauseMenuHandler",
                                "titleY",
                                "()I",
                                false));
                patched = true;
                break;
            }

            if (patched) {
                break;
            }
        }

        if (!patched) {
            throw new RuntimeException(
                    "Giscraft could not center the pause menu title.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
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
