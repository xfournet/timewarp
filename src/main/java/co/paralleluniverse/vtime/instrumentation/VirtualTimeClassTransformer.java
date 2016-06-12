/*
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime.instrumentation;

import java.util.*;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import co.paralleluniverse.vtime.VirtualClock;

/**
 * @author pron
 */
public class VirtualTimeClassTransformer extends ASMClassFileTransformer {
    private static final String PACKAGE = VirtualClock.class.getPackage().getName().replace('.', '/');
    private static final String CLOCK = Type.getInternalName(ClockProxy.class);

    private final Set<String> m_includedMethods;

    public VirtualTimeClassTransformer(Set<String> includedMethods) {
        m_includedMethods = includedMethods;
    }

    @Override
    protected boolean accept(String className) {
        return className != null && !className.startsWith(PACKAGE);
    }

    @Override
    protected ClassVisitor createVisitor(ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM5, next) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (!captureTimeCall(owner, name, desc)) {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    }

                    private boolean captureTimeCall(String owner, String name, String desc) {
                        if (m_includedMethods != null && !m_includedMethods.contains(name)) {
                            return false;
                        }

                        switch (owner) {
                            case "java/lang/Object":
                                if ("wait".equals(name)) {
                                    return callClockMethod("Object_wait", instanceToStatic(owner, desc));
                                }
                                break;
                            case "java/lang/System":
                                switch (name) {
                                    case "nanoTime":
                                        return callClockMethod("System_nanoTime", desc);
                                    case "currentTimeMillis":
                                        return callClockMethod("System_currentTimeMillis", desc);
                                }
                                break;
                            case "java/lang/Thread":
                                if ("sleep".equals(name)) {
                                    return callClockMethod("Thread_sleep", desc);
                                }
                                break;
                            case "sun/misc/Unsafe":
                                if ("park".equals(name)) {
                                    return callClockMethod("Unsafe_park", instanceToStatic(owner, desc));
                                }
                                break;
                            case "java/lang/management/RuntimeMXBean":
                                if ("getStartTime".equals(name)) {
                                    return callClockMethod("RuntimeMXBean_getStartTime", instanceToStatic(owner, desc));
                                }
                                break;
                        }
                        return false;
                    }

                    private boolean callClockMethod(String name, String desc) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, CLOCK, name, desc, false);
                        return true;
                    }

                    private String instanceToStatic(String owner, String desc) {
                        return "(L" + owner + ";" + desc.substring(1);
                    }
                };
            }
        };
    }
}
