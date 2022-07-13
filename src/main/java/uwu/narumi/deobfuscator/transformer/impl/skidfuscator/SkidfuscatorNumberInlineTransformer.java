package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionMatcher;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ShadowNullWow 13/07/2022
 */
public class SkidfuscatorNumberInlineTransformer extends Transformer {

    private static final InstructionMatcher STATIC_MATCHER = InstructionMatcher.of(LDC, LDC, GETSTATIC, ILOAD);
    private static final InstructionMatcher NORMAL_MATCHER = InstructionMatcher.of(LDC, LDC, ALOAD, GETFIELD, ILOAD);
    private static final Map<FieldNode, Integer> inlineFields = new HashMap<>();

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods
                    .stream()
                    .filter(methodNode -> !methodNode.name.equals("<init>") && !methodNode.name.equals("<clinit>"))
                    .forEach(methodNode -> {
                if (Modifier.isStatic(methodNode.access)) {
                    if (methodNode.instructions.size() < 4)
                        return;

                    if (STATIC_MATCHER.match(methodNode.instructions.get(0))) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) methodNode.instructions.get(2);
                        FieldNode fieldNode = findField(classNode, fd -> fd.name.equals(fieldInsnNode.name)
                                && fd.desc.equals(fieldInsnNode.desc)).orElse(null);
                        Integer value = getInlineValue(classNode, fieldNode);
                        if (value != null) {
                            methodNode.instructions.set(methodNode.instructions.get(2), getNumber(value));
                        }
                    }
                } else {
                    if (methodNode.instructions.size() < 5)
                        return;

                    if (NORMAL_MATCHER.match(methodNode.instructions.get(0))) {

                    }
                }
            });

            classNode.fields.removeIf(inlineFields::containsKey);
        });
    }

    private Integer getInlineValue(ClassNode classNode, FieldNode fieldNode) {
        if (inlineFields.containsKey(fieldNode)) {
            return inlineFields.get(fieldNode);
        }

        classNode.methods
                .stream()
                .filter(methodNode -> methodNode.name.equals("<init>"))
                .forEach(methodNode -> {
                    for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                        if (isInteger(ain) && ain.getNext() != null && ain.getNext().getOpcode() == PUTFIELD) {
                            FieldInsnNode fieldInsnNode = (FieldInsnNode) ain.getNext();
                            if (fieldInsnNode.name.equals(fieldNode.name)
                                && fieldInsnNode.desc.equals(fieldNode.desc)
                                && fieldInsnNode.owner.equals(classNode.name)) {
                                inlineFields.put(fieldNode, getInteger(ain));
                                methodNode.instructions.remove(ain.getNext());
                                methodNode.instructions.remove(ain);
                                break;
                            }
                        }
                    }
                });

        return inlineFields.get(fieldNode);
    }

}
