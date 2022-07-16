package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.asm.InstructionMatcher;
import uwu.narumi.deobfuscator.asm.InstructionModifier;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.DeadCodeRemoveTransformer;

import java.util.*;

public class SkidfuscatorFlowTransformer extends Transformer {

    private boolean forClinit;
    private static InstructionMatcher matcher = InstructionMatcher.of(NEW, DUP, INVOKESPECIAL, ATHROW);

    public SkidfuscatorFlowTransformer(boolean forClinit) {
        this.forClinit = forClinit;
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().parallelStream().forEach(classNode -> {
            if (forClinit) {
                classNode.methods
                        .stream()
                        .filter(methodNode -> methodNode.name.equals("<clinit>"))
                        .forEach(methodNode -> {
                            boolean modified;
                            do {
                                modified = false;

                                // Local inline
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
                                            Integer result = solveValue(frameMap, methodNode, varInsnNode, index);
                                            if (result == null) {
                                                continue;
                                            }

                                            modifier.remove(ain.getPrevious().getPrevious().getPrevious());
                                            modifier.remove(ain.getPrevious().getPrevious());
                                            modifier.replace(ain.getPrevious(), getNumber(result));
                                            modified = true;
                                        }
                                    } else if (ain.getOpcode() == ILOAD) {
                                        VarInsnNode varInsnNode = (VarInsnNode) ain;
                                        if (varInsnNode.var == index) {
                                            Integer result = getLocalValue(frameMap, methodNode, varInsnNode, index);
                                            if (result == null) {
                                                continue;
                                            }

                                            modifier.replace(ain, getNumber(result));
                                            modified = true;
                                        }
                                    }
                                }

                                modifier.apply(methodNode);

                                // number
                                for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                                    if (node.getOpcode() == INEG || node.getOpcode() == LNEG) {
                                        if (isInteger(node.getPrevious())) {
                                            int number = -getInteger(node.getPrevious());

                                            methodNode.instructions.remove(node.getPrevious());
                                            methodNode.instructions.set(node, getNumber(number));
                                            modified = true;
                                        } else if (isLong(node.getPrevious())) {
                                            long number = -getLong(node.getPrevious());

                                            methodNode.instructions.remove(node.getPrevious());
                                            methodNode.instructions.set(node, getNumber(number));
                                            modified = true;
                                        }
                                    } else if ((node.getOpcode() >= IADD && node.getOpcode() <= LXOR)) {
                                        if (isInteger(node.getPrevious().getPrevious()) && isInteger(node.getPrevious())) {
                                            int first = getInteger(node.getPrevious().getPrevious());
                                            int second = getInteger(node.getPrevious());

                                            Integer product = MathHelper.doMath(node.getOpcode(), first, second);
                                            if (product != null) {
                                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                                methodNode.instructions.remove(node.getPrevious());
                                                methodNode.instructions.set(node, getNumber(product));
                                                modified = true;
                                            }
                                        } else if (isLong(node.getPrevious().getPrevious()) && isLong(node.getPrevious())) {
                                            long first = getLong(node.getPrevious().getPrevious());
                                            long second = getLong(node.getPrevious());

                                            Long product = MathHelper.doMath(node.getOpcode(), first, second);
                                            if (product != null) {
                                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                                methodNode.instructions.remove(node.getPrevious());
                                                methodNode.instructions.set(node, getNumber(product));
                                                modified = true;
                                            }
                                        } else if ((isLong(node.getPrevious().getPrevious()) && isInteger(node.getPrevious()))) {
                                            long first = getLong(node.getPrevious().getPrevious());
                                            long second = getInteger(node.getPrevious());

                                            Long product = MathHelper.doMath(node.getOpcode(), first, second);
                                            if (product != null) {
                                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                                methodNode.instructions.remove(node.getPrevious());
                                                methodNode.instructions.set(node, getNumber(product));
                                                modified = true;
                                            }
                                        } else if ((isInteger(node.getPrevious().getPrevious()) && isLong(node.getPrevious()))) {
                                            long first = getInteger(node.getPrevious().getPrevious());
                                            long second = getLong(node.getPrevious());

                                            Long product = MathHelper.doMath(node.getOpcode(), first, second);
                                            if (product != null) {
                                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                                methodNode.instructions.remove(node.getPrevious());
                                                methodNode.instructions.set(node, getNumber(product));
                                                modified = true;
                                            }
                                        }
                                    }
                                }

                                // Flow
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
                                        modified = true;
                                    }
                                    if (ain instanceof LookupSwitchInsnNode && isInteger(ain.getPrevious())) {
                                        resolveLookup((LookupSwitchInsnNode) ain, methodNode, ain.getPrevious());
                                        modified = true;
                                    } else if (ain instanceof TableSwitchInsnNode && isInteger(ain.getPrevious())) {
                                        resolveTable((TableSwitchInsnNode) ain, methodNode, ain.getPrevious());
                                        modified = true;
                                    }
                                    if (ain.getOpcode() == GOTO) {
                                        JumpInsnNode jumpInsnNode = (JumpInsnNode) ain;
                                        if (getNext(ain) == getNext(jumpInsnNode.label)) {
                                            methodNode.instructions.remove(jumpInsnNode);
                                        }
                                    }
                                }

                                // cleanup ..
                                // so slow
                                Frame<BasicValue>[] frames;
                                try {
                                    frames = new Analyzer<>(new BasicInterpreter()).analyze(classNode.name, methodNode);
                                } catch (AnalyzerException e) {
                                    return;
                                }

                                List<AbstractInsnNode> remove = new ArrayList<>();

                                for (int i = 0; i < methodNode.instructions.size(); i++) {
                                    if (!isInstruction(methodNode.instructions.get(i)))
                                        continue;
                                    if (frames[i] != null)
                                        continue;

                                    remove.add(methodNode.instructions.get(i));
                                }

                                remove.forEach(methodNode.instructions::remove);
                            } while (modified);
                        });
            }
        });
    }

    private LabelNode getBlockFrom(AbstractInsnNode node) {
        while (!(node.getPrevious() instanceof LabelNode)) {
            if (node.getPrevious() == null) {
                return null;
            }
            node = node.getPrevious();
        }

        return (LabelNode) node.getPrevious();
    }

    private AbstractInsnNode nextLabel(AbstractInsnNode node) {
        AbstractInsnNode next = node.getNext();
        while (!(next instanceof LabelNode)) {
            next = next.getNext();
        }
        return next;
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

    private void resolveTable(TableSwitchInsnNode node, MethodNode methodNode, AbstractInsnNode keyNode) {
        int key = getInteger(keyNode);
        LabelNode originalCodeBlockStart = key < node.min || key > node.max ? node.dflt : node.labels.get(key - node.min);

        methodNode.instructions.remove(keyNode);
        methodNode.instructions.set(node, new JumpInsnNode(GOTO, originalCodeBlockStart));
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

    private boolean isInstruction(AbstractInsnNode node) {
        return !(node instanceof LineNumberNode) && !(node instanceof FrameNode) && !(node instanceof LabelNode);
    }

    private boolean isDoubleIf(AbstractInsnNode node) {
        return node.getOpcode() >= IF_ICMPEQ && node.getOpcode() <= IF_ICMPLE;
    }

    private Integer getLocalValue(Map<AbstractInsnNode, Frame<SourceValue>> frameMap, MethodNode methodNode, AbstractInsnNode cur, int curIndex) {
        Frame<SourceValue> frame = frameMap.get(cur);
        Set<AbstractInsnNode> insns = frame.getLocal(curIndex).insns;
        if (insns.size() != 1) {
            LabelNode labelNode = getBlockFrom(cur);
            if (labelNode == null) {
                return null;
            }

            TryCatchBlockNode blockNode = methodNode.tryCatchBlocks.stream().filter(block -> block.handler == labelNode).findFirst().orElse(null);
            if (blockNode == null) {
                return null;
            }
            return getLocalValue(frameMap, methodNode, blockNode.start.getNext(), curIndex);
        }

        AbstractInsnNode next = insns.iterator().next();
        return solveValue(frameMap, methodNode, next, curIndex);
    }

    private Integer solveValue(Map<AbstractInsnNode, Frame<SourceValue>> frameMap, MethodNode methodNode, AbstractInsnNode cur, int curIndex) {
        if (isInteger(cur.getPrevious())) {
            return getInteger(cur.getPrevious());
        } else if (cur.getPrevious().getOpcode() == IXOR) {
            int value1 = getInteger(cur.getPrevious().getPrevious().getPrevious());
            Integer value2 = getLocalValue(frameMap, methodNode, cur.getPrevious().getPrevious(), curIndex);
            if (value2 == null) {
                return null;
            }

            return value1 ^ value2;
        }

        return null;
    }

}
