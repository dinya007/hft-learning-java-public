package com.tisov.denis.jit;

public class InliningMega {

    public static void run() {
        for (int i = 0; i < 10_000_000; i++) {
            result += megaProcess(i);
        }
        System.out.println(result);
    }

    private static long result = 0;

    private static int step1(int x) {
        return x * 2;
    }

    private static int step2(int x) {
        return x + 3;
    }

    private static int step3(int x) {
        return x * 4;
    }

    private static int megaProcess(int x) {
        return megaPart1(x) + megaPart2(x);
    }

    private static int megaPart1(int x) {
        int a = x * 2 + 1;
        int b = a * 3 - 2;
        int c = b * 4 + 3;
        int d = c * 5 - 4;
        int e = d * 6 + 5;
        int f = e * 7 - 6;
        int g = f * 8 + 7;
        int h = g * 9 - 8;
        int i = h * 10 + 9;
        int j = i * 11 - 10;
        int k = j * 12 + 11;
        int l = k * 13 - 12;
        return a + b + c + d + e + f + g + h + i + j + k + l;
    }

    private static int megaPart2(int x) {
        int m = x * 14 + 13;
        int n = m * 15 - 14;
        int o = n * 16 + 15;
        int p = o * 17 - 16;
        int q = p * 18 + 17;
        int s = q * 19 - 18;
        int t = s * 20 + 19;
        int u = t * 21 - 20;
        int v = u * 22 + 21;
        int w = v * 23 - 22;
        int y = w * 24 + 23;
        int z = y * 25 - 24;
        int aa = z * 26 + 25;
        int bb = aa * 27 - 26;
        int cc = bb * 28 + 27;
        return m + n + o + p + q + s + t + u + v + w + y + z + aa + bb + cc
                + step1(x) + step2(x) + step3(x);
    }

}
