package com.tisov.denis.jit;

public class Inlining {

    private static long result = 0;

    public static void run() {
        for (int i = 0; i < 10_000_000; i++) {
            result += bigMethod(i);
        }
        System.out.println(result);
    }

    private static int bigMethod(int x) {
        int a = x * 2;
        int b = a + 3;
        int c = b * 4;
        int d = c - 5;
        int e = d * 6;
        int f = e + 7;
        int g = f * 8;
        int h = g - 9;
        int i = h * 10;
        int j = i + 11;
        return a + b + c + d + e + f + g + h + i + j;
    }

    private static int smallMethod(int x) {
        return x * x + 1;
    }

}
