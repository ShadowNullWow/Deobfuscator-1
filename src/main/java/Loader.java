import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.skidfuscator.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.DeadCodeRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("test", "Evaluator.jar-out.jar"))
                .output(Path.of("test", "Evaluator.jar-out-deobf.jar"))
                .transformers(
                        new DeadCodeRemoveTransformer(),
                        new SkidfuscatorWatermarkRemoveTransformer(),
                        new SkidfuscatorNumberTransformer(),
//                        new SkidfuscatorLocalInlineTransformer(true),
//                        new UniversalNumberTransformer(),
                        new SkidfuscatorFlowTransformer(true)
//                        new DeadCodeRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}