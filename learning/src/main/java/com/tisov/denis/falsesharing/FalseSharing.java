package com.tisov.denis.falsesharing;

import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class FalseSharing {

    @Contended
    private long a;
    @Contended
    private long b;
    private long c;
    private long d;

    private static final VarHandle A_VAR;
    private static final VarHandle B_VAR;
    private static final VarHandle C_VAR;
    private static final VarHandle D_VAR;

    static {
        try {
            A_VAR = MethodHandles.lookup().findVarHandle(FalseSharing.class, "a", long.class);
            B_VAR = MethodHandles.lookup().findVarHandle(FalseSharing.class, "b", long.class);
            C_VAR = MethodHandles.lookup().findVarHandle(FalseSharing.class, "c", long.class);
            D_VAR = MethodHandles.lookup().findVarHandle(FalseSharing.class, "d", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public long incrementA() {
        return (long) A_VAR.getAndAdd(this, 1L);
    }

    public long incrementB() {
        return (long) B_VAR.getAndAdd(this, 1L);
    }

    public long incrementC() {
        return (long) C_VAR.getAndAdd(this, 1L);
    }

    public long incrementD() {
        return (long) D_VAR.getAndAdd(this, 1L);
    }

}
