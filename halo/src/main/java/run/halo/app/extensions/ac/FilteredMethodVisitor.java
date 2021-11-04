package run.halo.app.extensions.ac;

import static run.halo.app.extensions.ac.NamingUtils.toSourceCodeFormat;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author guqing
 * @since 2021-11-04
 */
public class FilteredMethodVisitor extends MethodVisitor {

    private final Method sourceMethod;

    private final Collector collect;

    public FilteredMethodVisitor(final Method sourceMethod,
        final MethodVisitor methodVisitor,
        final Collector collect) {
        super(Opcodes.ASM7, methodVisitor);
        this.sourceMethod = sourceMethod;
        this.collect = collect;
    }

    @Override
    public void visitMethodInsn(final int opcode,
        final String owner,
        final String name,
        final String descriptor,
        final boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        handleTargetMethod(owner, name, descriptor);
    }

    @Override
    public void visitInvokeDynamicInsn(final String name,
        final String descriptor,
        final Handle handle,
        final Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, handle, bootstrapMethodArguments);
        handleTargetMethod(handle.getOwner(), handle.getName(), handle.getDesc());
    }

    private void handleTargetMethod(final String targetClassName,
        final String targetMethodName,
        final String targetMethodDescriptor) {
        final Method targetMethod = new Method.Builder()
            .className(cleanseClassName(toSourceCodeFormat(targetClassName)))
            .methodName(targetMethodName)
            .build();

        collect.collectMethodCoupling(
            new MethodCoupling.Builder()
                .source(sourceMethod)
                .target(targetMethod)
                .build()
        );
    }

    static String cleanseClassName(final String methodName) {
        if (methodName.startsWith("WEB-INF.classes.")) {
            return methodName.substring(16);
        }

        return methodName;
    }
}
