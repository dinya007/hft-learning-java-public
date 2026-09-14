package com.tisov.denis.jit;

public class IntLoop {

    public static void safePoint() throws InterruptedException {
        Thread worker = new Thread(() -> {
            int res = 0;
            for (int i = 0; i < 2_000_000_000; i++) {
                res += i;
            }
            IO.println(res);
        });

        Thread trigger = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.gc();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        worker.start();
        trigger.start();

        worker.join();
        trigger.join();

    }

}
