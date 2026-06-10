package com.example.sqs.batch.load;

final class CountSplitter {

    private CountSplitter() {
    }

    static int[] split(int total, int threads) {
        int[] counts = new int[threads];
        int base = total / threads;
        int remainder = total % threads;
        for (int i = 0; i < threads; i++) {
            counts[i] = base + (i < remainder ? 1 : 0);
        }
        return counts;
    }
}
