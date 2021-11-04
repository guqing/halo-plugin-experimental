package run.halo.app.extensions.ac;

import static run.halo.app.extensions.ac.FilteredMethodVisitor.cleanseClassName;
import static run.halo.app.extensions.ac.NamingUtils.toSourceCodeFormat;

import java.io.IOException;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author guqing
 * @since 2021-11-04
 */
public class FilteredClassVisitor extends ClassVisitor {

    private final String className;

    private final ClassReader reader;

    private final Collector collect;

    public FilteredClassVisitor(final String className, final Collector collect) throws
        IOException {
        this(className, collect, new ClassReader(className));
    }

    public FilteredClassVisitor(final String className, final Collector collect,
        final byte[] classData) {
        this(className, collect, new ClassReader(classData));
    }

    private FilteredClassVisitor(final String className, final Collector collect,
        final ClassReader classReader) {
        super(Opcodes.ASM7);

        Objects.requireNonNull(className);
        Objects.requireNonNull(collect);

        this.className = cleanseClassName(toSourceCodeFormat(className));
        this.collect = collect;

        this.reader = classReader;
    }

    /**
     * This will scan this class and visit all the method contents to scan for dependencies.
     */
    public void visit() {
        reader.accept(this, 0);
    }

    @Override
    public MethodVisitor visitMethod(final int access,
        final String name,
        final String descriptor,
        final String signature,
        final String[] exceptions) {
        final MethodVisitor
            methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        final Method method = new Method.Builder()
            .className(className)
            .methodName(name)
            .build();
        return new FilteredMethodVisitor(method, methodVisitor, collect);
    }
}
