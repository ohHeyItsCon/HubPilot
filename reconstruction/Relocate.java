import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Build-only relocation. ASM and tooling are never shipped in plugin artifacts. */
public final class Relocate {
    private static final String ORIGINAL = "org/yaml/snakeyaml";
    private static final String PRIVATE = "dev/hubpilot/libs/snakeyaml";
    private static final Remapper REMAPPER = new Remapper() {
        @Override public String map(String name) {
            return name.startsWith(ORIGINAL + "/") ? PRIVATE + name.substring(ORIGINAL.length()) : name;
        }
        @Override public Object mapValue(Object value) {
            if (value instanceof String text && text.startsWith("org.yaml.snakeyaml.")) {
                return "dev.hubpilot.libs.snakeyaml." + text.substring("org.yaml.snakeyaml.".length());
            }
            return super.mapValue(value);
        }
    };
    private static byte[] relocate(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassRemapper(writer, REMAPPER), 0);
        return writer.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        Path classes = Path.of(args[0]);
        try (var paths = Files.walk(classes)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                Files.write(path, relocate(Files.readAllBytes(path)));
            }
        }
        try (JarFile jar = new JarFile(args[1])) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith(ORIGINAL + "/") || !entry.getName().endsWith(".class")) continue;
                Path target = classes.resolve(PRIVATE + entry.getName().substring(ORIGINAL.length()));
                Files.createDirectories(target.getParent());
                try (var stream = jar.getInputStream(entry)) {
                    Files.write(target, relocate(stream.readAllBytes()));
                }
            }
        }
    }
}
