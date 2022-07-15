package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionMatcher;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class SkidfuscatorFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                    if (isDoubleIf(ain) && isInteger(ain.getPrevious()) && isInteger(ain.getPrevious().getPrevious())) {
                        int arg1 = getInteger(ain.getPrevious().getPrevious());
                        int arg2 = getInteger(ain.getPrevious());
                        if (runDoubleIf(arg1, arg2, ain.getOpcode())) {
                            methodNode.instructions.remove(ain.getPrevious().getPrevious());
                            methodNode.instructions.remove(ain.getPrevious());
                            JumpInsnNode jumpInsnNode = (JumpInsnNode) ain;
                            jumpInsnNode.setOpcode(GOTO);
                        } else {
                            methodNode.instructions.remove(ain.getPrevious().getPrevious());
                            methodNode.instructions.remove(ain.getPrevious());
                            methodNode.instructions.remove(ain);
                        }
                    }
                    if (ain instanceof LookupSwitchInsnNode && isInteger(ain.getPrevious())) {
                        resolveLookup((LookupSwitchInsnNode) ain, methodNode, ain.getPrevious());
                    }
                }
            });
        });
    }

    private AbstractInsnNode getNext(AbstractInsnNode node) {
        AbstractInsnNode next = node.getNext();
        while (next instanceof LineNumberNode || next instanceof FrameNode || next instanceof LabelNode) {
            next = next.getNext();
        }
        return next;
    }

    private void resolveLookup(LookupSwitchInsnNode node, MethodNode methodNode, AbstractInsnNode keyNode) {
        int key = getInteger(keyNode);
        LabelNode result = node.keys.contains(key) ? node.labels.get(node.keys.indexOf(key)) : node.dflt;

        node.labels.stream().filter(nd -> nd != result).forEach(nd -> removeBlock(methodNode, nd));
        if (result != node.dflt) {
            removeBlock(methodNode, node.dflt);
        }

        methodNode.instructions.remove(keyNode);
        methodNode.instructions.set(node, new JumpInsnNode(GOTO, result));
    }

    private void removeBlock(MethodNode methodNode, LabelNode label) {
        AbstractInsnNode next = label.getNext();
        while (next.getType() != AbstractInsnNode.LABEL) {
            if (next.getNext() == null) {
                methodNode.instructions.remove(next);
                break;
            } else {
                next = next.getNext();
                methodNode.instructions.remove(next.getPrevious());
            }
        }
    }

    private boolean runDoubleIf(int value1, int value2, int opcode) {
        switch (opcode) {
            case IF_ICMPEQ:
                return value1 == value2;
            case IF_ICMPNE:
                return value1 != value2;
            case IF_ICMPLT:
                return value1 < value2;
            case IF_ICMPGE:
                return value1 >= value2;
            case IF_ICMPGT:
                return value1 > value2;
            case IF_ICMPLE:
                return value1 <= value2;
        }

        throw new RuntimeException();
    }

    private boolean isDoubleIf(AbstractInsnNode node) {
        return node.getOpcode() >= IF_ICMPEQ && node.getOpcode() <= IF_ICMPLE;
    }

}
