import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.skidfuscator.SkidfuscatorNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.skidfuscator.SkidfuscatorWatermarkRemoveTransformer;
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
                        new SkidfuscatorNumberTransformer()
//                        new SkidfuscatorNumberInlineTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}