/*
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are license under the terms of the
 * MIT license.
 */
package co.paralleluniverse.vtime;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import co.paralleluniverse.vtime.boot.ClockProxy;

/**
 * @author pron
 */
class VirtualTimeClassTransformer implements ClassFileTransformer {
    private static final String PACKAGE = VirtualClock.class.getPackage().getName().replace('.', '/');
    private static final String CLOCK = Type.getInternalName(ClockProxy.class);

    private final Set<String> includedMethods;

    VirtualTimeClassTransformer(Set<String> includedMethods) {
        this.includedMethods = includedMethods;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            if (accept(className)) {
                return instrumentClass(classfileBuffer);
            } else {
                return null;
            }
        } catch (Throwable t) {
            Logger.warning("Instrumentation by %s failed for class %s:", t, getClass().getName(), Type.getType(classBeingRedefined).getClassName());
            throw t; // same effect as returning null
        }
    }

    private boolean accept(String className) {
        return className != null && !className.startsWith(PACKAGE);
    }

    private ClassVisitor createVisitor(ClassVisitor next) {
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
                        if (includedMethods == null || includedMethods.contains(name)) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, CLOCK, name, desc, false);
                            return true;
                        } else {
                            return false;
                        }
                    }

                    private String instanceToStatic(String owner, String desc) {
                        return "(L" + owner + ";" + desc.substring(1);
                    }
                };
            }
        };
    }

    private byte[] instrumentClass(byte[] classfileBuffer) {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = createVisitor(cw);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
