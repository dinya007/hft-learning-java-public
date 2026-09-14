package com.tisov.denis.jit;

public class RunningApp {

    private long sink;

    public void run() throws Exception {
        long pid = ProcessHandle.current().pid();
        System.out.println("PID = " + pid);
        System.out.println("Run: jcmd " + pid + " Thread.print | grep -A1 -i compilerthread");

        for (int round = 0; round < 5; round++) {
            cycle();
        }

        System.out.println("warmup done, acc = " + sink);

        while (System.in.read() != 'e') {
            System.out.println("Press 'e' to exit");
        }
    }

    private void cycle() {
        long acc = 0;
        for (int i = 0; i < 4_000_000; i++) {
            acc = compute(acc, i);
        }
        sink = acc;
    }

    private long compute(long acc, long x) {
        long m = x * 2654435761L;
        m ^= (m >>> 17);
        m *= 0xBF58476D1CE4E5B9L;
        m ^= (m >>> 31);
        return acc + m;
    }
}
