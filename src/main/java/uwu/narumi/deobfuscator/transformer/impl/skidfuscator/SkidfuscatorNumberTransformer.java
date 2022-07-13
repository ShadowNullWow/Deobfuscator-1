package uwu.narumi.deobfuscator.transformer.impl.skidfuscator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class SkidfuscatorNumberTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                boolean modified;
                do {
                    modified = false;

                    for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                        // Cast and Poly Number
                        if (ain.getNext() != null) {
                            // Cast
                            if (isInteger(ain) && ain.getNext().getOpcode() == I2L) {
                                long result = (long) getInteger(ain);
                                methodNode.instructions.set(ain.getNext(), getNumber(result));
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            } else if (isLong(ain) && ain.getNext().getOpcode() == L2I) {
                                int result = (int) getLong(ain);
                                methodNode.instructions.set(ain.getNext(), getNumber(result));
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            }

                            // Poly Number
                            if (isString(ain)
                                && isMethod(ain.getNext(), "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", INVOKESTATIC)) {
                                int result = Integer.parseInt(getString(ain));
                                methodNode.instructions.set(ain.getNext(), getNumber(result));
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            }
                        }

                        // Int - Multi
                        // e.g. 12 * 13
                        if (isInteger(ain) && isInteger(ain.getNext())
                                && ain.getNext().getNext() != null && isIntMultiOperation(ain.getNext().getNext())) {
                            int value1 = getInteger(ain);
                            int value2 = getInteger(ain.getNext());
                            Integer result = calculate(value1, value2, ain.getNext().getNext().getOpcode());
                            if (result != null) {
                                methodNode.instructions.set(ain.getNext().getNext(), getNumber(result));
                                methodNode.instructions.remove(ain.getNext());
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            }
                        }

                        // Int - Single
                        // e.g. -(-12)
                        if (isInteger(ain) && ain.getNext() != null && isIntSingleOperation(ain.getNext())) {
                            int value = getInteger(ain);
                            Integer result = calculate(value, ain.getNext().getOpcode());
                            if (result != null) {
                                methodNode.instructions.set(ain.getNext(), getNumber(result));
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            }
                        }

                        // Long
                        // e.g. 114514L * 1919810L
                        if (isLong(ain) && isLong(ain.getNext())
                                && ain.getNext().getNext() != null && isLongMultiOperation(ain.getNext().getNext())) {
                            long value1 = getLong(ain);
                            long value2 = getLong(ain.getNext());
                            Long result = calculate(value1, value2, ain.getNext().getNext().getOpcode());
                            if (result != null) {
                                methodNode.instructions.set(ain.getNext().getNext(), getNumber(result));
                                methodNode.instructions.remove(ain.getNext());
                                methodNode.instructions.remove(ain);
                                modified = true;
                                continue;
                            }
                        }
                    }
                } while (modified);
            });
        });
    }

    private boolean isIntSingleOperation(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();
        return opcode == Opcodes.INEG;
    }

    private boolean isIntMultiOperation(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();
        return opcode == Opcodes.IADD || opcode == Opcodes.ISUB || opcode == Opcodes.IMUL
                || opcode == Opcodes.IDIV || opcode == Opcodes.IOR || opcode == Opcodes.IXOR
                || opcode == Opcodes.IAND || opcode == Opcodes.ISHL || opcode == Opcodes.ISHR
                || opcode == Opcodes.IUSHR || opcode == Opcodes.IREM;
    }

    private boolean isLongMultiOperation(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();
        return opcode == Opcodes.LADD || opcode == Opcodes.LSUB || opcode == Opcodes.LMUL
                || opcode == Opcodes.LDIV || opcode == Opcodes.LOR || opcode == Opcodes.LXOR
                || opcode == Opcodes.LAND || opcode == Opcodes.LREM || opcode == Opcodes.LSHL
                || opcode == Opcodes.LSHR || opcode == Opcodes.LUSHR;
    }

    private Integer calculate(int value1, int opcode) {
        switch (opcode) {
            case INEG:
                return -value1;
        }
        return null;
    }

    private Integer calculate(int value1, int value2, int opcode) {
        switch (opcode) {
            case IADD:
                return value1 + value2;
            case ISUB:
                return value1 - value2;
            case IMUL:
                return value1 * value2;
            case IDIV:
                if (value2 == 0)
                    return null;
                return value1 / value2;
            case IAND:
                return value1 & value2;
            case IOR:
                return value1 | value2;
            case IXOR:
                return value1 ^ value2;
            case IREM:
                return value1 % value2;
            case ISHL:
                return value1 << value2;
            case ISHR:
                return value1 >> value2;
            case IUSHR:
                return value1 >>> value2;
        }
        return null;
    }

    private Long calculate(long value1, long value2, int opcode) {
        switch (opcode) {
            case LADD:
                return value1 + value2;
            case LSUB:
                return value1 - value2;
            case LMUL:
                return value1 * value2;
            case LDIV:
                if (value2 == 0)
                    return null;
                return value1 / value2;
            case LAND:
                return value1 & value2;
            case LOR:
                return value1 | value2;
            case LXOR:
                return value1 ^ value2;
            case LREM:
                return value1 % value2;
            case LSHL:
                return value1 << value2;
            case LSHR:
                return value1 >> value2;
            case LUSHR:
                return value1 >>> value2;
        }
        return null;
    }

}
