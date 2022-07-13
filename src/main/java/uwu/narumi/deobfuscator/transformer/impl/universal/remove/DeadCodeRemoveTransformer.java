package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.analysis.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DeadCodeRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        AtomicInteger deadInstructions = new AtomicInteger();

        deobfuscator.classes().parallelStream().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                if (methodNode.instructions.getFirst() == null)
                    return;

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
                    deadInstructions.getAndIncrement();
                }

                remove.forEach(methodNode.instructions::remove);
            });
        });
    }

    private boolean isInstruction(AbstractInsnNode node) {
        return !(node instanceof LineNumberNode) && !(node instanceof FrameNode) && !(node instanceof LabelNode);
    }

}
