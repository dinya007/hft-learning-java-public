package com.tisov.denis.disruptor.utils;

import com.tisov.denis.disruptor.sequence.Sequence;

public class SequenceUtils {

    public static long min(Sequence[] sequences) {
        return min(sequences, Long.MAX_VALUE);
    }

    public static long min(Sequence[] sequences, long min) {
        for (Sequence sequence : sequences) {
            long value = sequence.get();
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

}
