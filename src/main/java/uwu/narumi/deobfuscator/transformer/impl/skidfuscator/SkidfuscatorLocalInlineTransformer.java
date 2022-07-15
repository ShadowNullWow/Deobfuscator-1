package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionModifier;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Map;
import java.util.Set;

public class SkidfuscatorLocalInlineTransformer extends Transformer {

    private boolean forClinit;

    public SkidfuscatorLocalInlineTransformer(boolean forClinit) {
        this.forClinit = forClinit;
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            if (forClinit) {
                classNode.methods
                        .parallelStream()
                        .filter(methodNode -> methodNode.name.equals("<clinit>"))
                        .forEach(methodNode -> {
                            if (methodNode.instructions.size() < 2)
                                return;

                            int index = -1;
                            int value = 0;
                            if (isInteger(methodNode.instructions.get(0))
                                    && methodNode.instructions.get(1).getOpcode() == ISTORE) {
                                VarInsnNode varInsnNode = (VarInsnNode) methodNode.instructions.get(1);
                                index = varInsnNode.var;
                                value = getInteger(methodNode.instructions.get(0));
                            }

                            InstructionModifier modifier = new InstructionModifier();

                            Map<AbstractInsnNode, Frame<SourceValue>> frameMap = analyzeSource(classNode, methodNode);
                            for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                                if (ain.getOpcode() == ISTORE) {
                                    VarInsnNode varInsnNode = (VarInsnNode) ain;
                                    if (varInsnNode.var == index && varInsnNode.getPrevious().getOpcode() == IXOR) {
                                        Integer result = solveValue(frameMap, varInsnNode, index);
                                        if (result == null) {
                                            continue;
                                        }

                                        modifier.remove(ain.getPrevious().getPrevious().getPrevious());
                                        modifier.remove(ain.getPrevious().getPrevious());
                                        modifier.replace(ain.getPrevious(), getNumber(result));
                                    }
                                } else if (ain.getOpcode() == ILOAD) {
                                    VarInsnNode varInsnNode = (VarInsnNode) ain;
                                    if (varInsnNode.var == index) {
                                        Integer result = getLocalValue(frameMap, varInsnNode, index);
                                        if (result == null) {
                                            continue;
                                        }

                                        modifier.replace(ain, getNumber(result));
                                    }
                                }
                            }

                            modifier.apply(methodNode);
                        });
            }
        });
    }

    private Integer getLocalValue(Map<AbstractInsnNode, Frame<SourceValue>> frameMap, AbstractInsnNode cur, int curIndex) {
        Frame<SourceValue> frame = frameMap.get(cur);
        Set<AbstractInsnNode> insns = frame.getLocal(curIndex).insns;
        if (insns.size() != 1) {
            return null;
        }

        AbstractInsnNode next = insns.iterator().next();
        return solveValue(frameMap, next, curIndex);
    }

    private Integer solveValue(Map<AbstractInsnNode, Frame<SourceValue>> frameMap, AbstractInsnNode cur, int curIndex) {
        if (isInteger(cur.getPrevious())) {
            return getInteger(cur.getPrevious());
        } else if (cur.getPrevious().getOpcode() == IXOR) {
            int value1 = getInteger(cur.getPrevious().getPrevious().getPrevious());
            Integer value2 = getLocalValue(frameMap, cur.getPrevious().getPrevious(), curIndex);
            if (value2 == null) {
                return null;
            }

            return value1 ^ value2;
        }

        return null;
    }

}
