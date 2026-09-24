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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class RailSpeedTransformer implements IClassTransformer {
    private static final String TARGET_RAIL_CLASS = "net.minecraft.block.BlockRailBase";
    private static final String TARGET_CHEST_CLASS = "net.minecraft.block.BlockChest";
    private static final String TARGET_CHEST_MODEL =
            "net.minecraft.client.model.ModelChest";
    private static final String TARGET_LARGE_CHEST_MODEL =
            "net.minecraft.client.model.ModelLargeChest";
    private static final String TARGET_CHEST_RENDERER =
            "net.minecraft.client.renderer.tileentity.TileEntityChestRenderer";
    private static final String TARGET_METHOD = "getRailMaxSpeed";
    private static final String TARGET_DESC =
            "(Lnet/minecraft/world/World;Lnet/minecraft/entity/item/EntityMinecart;III)F";
    private static final String CONFIG_OWNER = "doc/fasterminecarts/RailSpeedConfig";
    private static final String CHEST_BOUNDS_DESC =
            "(Lnet/minecraft/world/IBlockAccess;III)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return basicClass;
        }

        if (TARGET_RAIL_CLASS.equals(transformedName)) {
            return transformRailSpeed(basicClass);
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

        return basicClass;
    }

    private static byte[] transformRailSpeed(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        new ClassReader(basicClass).accept(classNode, 0);

        boolean patched = false;

        for (MethodNode method : classNode.methods) {
            if (!TARGET_METHOD.equals(method.name) || !TARGET_DESC.equals(method.desc)) {
                continue;
            }

            for (AbstractInsnNode insn = method.instructions.getFirst();
                 insn != null;
                 insn = insn.getNext()) {

                if (insn instanceof LdcInsnNode) {
                    Object constant = ((LdcInsnNode) insn).cst;
                    if (constant instanceof Float
                            && Float.compare((Float) constant, 0.4F) == 0) {
                        method.instructions.set(insn,
                                new FieldInsnNode(
                                        Opcodes.GETSTATIC,
                                        CONFIG_OWNER,
                                        "speedBlocksPerTick",
                                        "F"));
                        patched = true;
                        break;
                    }
                }
            }

            break;
        }

        if (!patched) {
            throw new RuntimeException(
                    "FasterVanillaMinecarts could not patch BlockRailBase#getRailMaxSpeed. "
                            + "Another coremod may have changed the method.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
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
                    "FasterVanillaMinecarts could not patch BlockChest bounds.");
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
                    "FasterVanillaMinecarts could not expand "
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
                    "FasterVanillaMinecarts could not limit the chest lid angle.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

}
