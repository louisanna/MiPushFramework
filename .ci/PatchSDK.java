import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * 给 SDK jar 里的 PendingIntent.getActivity/getBroadcast/getService/getForegroundService
 * 调用补上 FLAG_IMMUTABLE，解决 targetSdk 31+ 的 IllegalArgumentException。
 */
public class PatchSDK {
    static final int FLAG_IMMUTABLE = 0x04000000;
    static final Set<String> METHODS = new HashSet<>(Arrays.asList(
            "getActivity", "getBroadcast", "getService", "getForegroundService"));

    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        int patchedMethods = 0;
        int patchedCalls = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = zis.readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    ClassReader cr = new ClassReader(data);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);
                    boolean changed = false;
                    for (MethodNode mn : cn.methods) {
                        for (AbstractInsnNode insn : mn.instructions) {
                            if (insn instanceof MethodInsnNode) {
                                MethodInsnNode m = (MethodInsnNode) insn;
                                if ("android/app/PendingIntent".equals(m.owner)
                                        && METHODS.contains(m.name)
                                        && m.desc.endsWith(")Landroid/app/PendingIntent;")) {
                                    mn.instructions.insertBefore(m, new LdcInsnNode(FLAG_IMMUTABLE));
                                    mn.instructions.insertBefore(m, new InsnNode(Opcodes.IOR));
                                    changed = true;
                                    patchedCalls++;
                                }
                            }
                        }
                    }
                    if (changed) {
                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        cn.accept(cw);
                        data = cw.toByteArray();
                        patchedMethods++;
                    }
                }
                ZipEntry ne = new ZipEntry(entry.getName());
                zos.putNextEntry(ne);
                zos.write(data);
                zos.closeEntry();
            }
        }
        System.out.println("patched classes=" + patchedMethods + " calls=" + patchedCalls);
    }
}
