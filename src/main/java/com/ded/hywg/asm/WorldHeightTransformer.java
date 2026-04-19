package com.ded.hywg.asm;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class WorldHeightTransformer implements IClassTransformer {

    private static final String ACL_CLASS = "net.minecraft.world.chunk.storage.AnvilChunkLoader";
    private static final String NBT = "net/minecraft/nbt/NBTTagCompound";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if (!transformedName.equals(ACL_CLASS)) return bytes;

        System.out.println("[CAM-ASM] Transforming AnvilChunkLoader...");
        boolean deobf = name.equals(transformedName);

        try {
            ClassNode cn = new ClassNode();
            new ClassReader(bytes).accept(cn, 0);

            for (MethodNode mn : cn.methods) {
                patchMethod(mn, deobf);
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            System.out.println("[CAM-ASM] AnvilChunkLoader patched OK");
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[CAM-ASM] FAILED to patch AnvilChunkLoader!");
            t.printStackTrace();
            return bytes;
        }
    }

    private void patchMethod(MethodNode mn, boolean deobf) {
        
        String setByteName = deobf ? "setByte" : "func_74774_a";
        String getByteName = deobf ? "getByte" : "func_74771_c";
        String setShortName = deobf ? "setShort" : "func_74777_a";
        String getShortName = deobf ? "getShort" : "func_74765_d";

        ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();

            
            if (isIntConst(insn, 16)) {
                AbstractInsnNode next = insn.getNext();
                if (next instanceof TypeInsnNode && next.getOpcode() == Opcodes.ANEWARRAY) {
                    String desc = ((TypeInsnNode) next).desc;
                    if (desc.contains("ExtendedBlockStorage")) {
                        mn.instructions.set(insn, makeIntInsn(WorldHeightConfig.CHUNK_SECTIONS));
                        System.out.println("[CAM-ASM]   " + mn.name + ": EBS[16] → EBS[" + WorldHeightConfig.CHUNK_SECTIONS + "]");
                    }
                }
            }

            
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if (mi.owner.equals(NBT)
                        && (mi.name.equals("setByte") || mi.name.equals(setByteName))
                        && mi.desc.equals("(Ljava/lang/String;B)V")) {
                    if (hasLdcBefore(insn, "Y", 10)) {
                        
                        AbstractInsnNode prev = insn.getPrevious();
                        if (prev != null && prev.getOpcode() == Opcodes.I2B) {
                            mn.instructions.set(prev, new InsnNode(Opcodes.I2S));
                        }
                        mi.name = mi.name.equals("setByte") ? "setShort" : setShortName;
                        mi.desc = "(Ljava/lang/String;S)V";
                        System.out.println("[CAM-ASM]   " + mn.name + ": setByte(Y) → setShort(Y)");
                    }
                }
            }

            
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                if (mi.owner.equals(NBT)
                        && (mi.name.equals("getByte") || mi.name.equals(getByteName))
                        && mi.desc.equals("(Ljava/lang/String;)B")) {
                    if (hasLdcBefore(insn, "Y", 5)) {
                        mi.name = mi.name.equals("getByte") ? "getShort" : getShortName;
                        mi.desc = "(Ljava/lang/String;)S";
                        System.out.println("[CAM-ASM]   " + mn.name + ": getByte(Y) → getShort(Y)");
                    }
                }
            }
        }
    }

    private boolean isIntConst(AbstractInsnNode insn, int value) {
        if (insn instanceof IntInsnNode) {
            return ((IntInsnNode) insn).operand == value;
        }
        if (insn instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) insn).cst;
            return cst instanceof Integer && (Integer) cst == value;
        }
        if (insn.getOpcode() >= Opcodes.ICONST_0 && insn.getOpcode() <= Opcodes.ICONST_5) {
            return (insn.getOpcode() - Opcodes.ICONST_0) == value;
        }
        return false;
    }

    private AbstractInsnNode makeIntInsn(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + value);
        }
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, value);
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }

    private boolean hasLdcBefore(AbstractInsnNode target, String value, int maxDist) {
        AbstractInsnNode cur = target;
        for (int i = 0; i < maxDist && cur != null; i++) {
            cur = cur.getPrevious();
            if (cur instanceof LdcInsnNode && value.equals(((LdcInsnNode) cur).cst)) {
                return true;
            }
        }
        return false;
    }
}
