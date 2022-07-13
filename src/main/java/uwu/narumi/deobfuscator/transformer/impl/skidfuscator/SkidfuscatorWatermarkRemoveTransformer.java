package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionMatcher;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class SkidfuscatorWatermarkRemoveTransformer extends Transformer {

    private static InstructionMatcher matcher = InstructionMatcher.of(BIPUSH, ANEWARRAY, PUTSTATIC);

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods
                    .stream()
                    .filter(methodNode -> methodNode.name.equals("<clinit>"))
                    .forEach(methodNode -> {
                        if (matcher.match(methodNode.instructions.get(0))) {
                            FieldInsnNode fieldInsnNode = (FieldInsnNode) methodNode.instructions.get(2);
                            FieldNode watermark = findField(classNode, fieldNode -> fieldNode.name.equals(fieldInsnNode.name) && fieldInsnNode.desc.equals("[Ljava/lang/String;")).orElse(null);
                            if (watermark != null) {
                                classNode.fields.remove(watermark);

                                AbstractInsnNode ain = matcher.matchAndGetLast(methodNode.instructions.get(0));
                                while (match(ain)) {
                                    ain = ain.getNext().getNext().getNext().getNext();
                                }

                                getInstructionsBetween(methodNode.instructions.get(0), ain, true, false)
                                        .forEach(methodNode.instructions::remove);
                            }
                        }
                    });
        });
    }

    private boolean match(AbstractInsnNode ain) {
        if (ain == null)
            return false;

        if (ain.getOpcode() == GETSTATIC
            && isInteger(ain.getNext())
            && ain.getNext().getNext() != null && isString(ain.getNext().getNext())
            && ain.getNext().getNext().getNext() != null && ain.getNext().getNext().getNext().getOpcode() == AASTORE) {
            return true;
        }

        return false;
    }

}
