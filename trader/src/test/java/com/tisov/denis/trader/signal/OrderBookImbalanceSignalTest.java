package com.tisov.denis.trader.signal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.tisov.denis.trader.signal.SignalSide.DOWN;
import static com.tisov.denis.trader.signal.SignalSide.FLAT;
import static com.tisov.denis.trader.signal.SignalSide.UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderBookImbalanceSignalTest {

    @ParameterizedTest(name = "bid={0} ask={1} -> {2}")
    @CsvSource({
            "  10,    1,   1",
            " 100,    0,   1",
            "   1,   10,  -1",
            "   0,  100,  -1",
            "   6,    4,   0",
            "   4,    6,   0",
            "   5,    5,   0",
            "   3,    1,   0",
            "   1,    3,   0",
    })
    void classifiesAgainstHalfThreshold(long bid, long ask, byte expected) {
        OrderBookImbalanceSignal signal = new OrderBookImbalanceSignal(1, 2);

        assertThat(signal.get(bid, ask)).isEqualTo(expected);
    }

    @Test
    void emptyBookIsFlat() {
        OrderBookImbalanceSignal signal = new OrderBookImbalanceSignal(1, 2);

        assertThat(signal.get(0, 0)).isEqualTo(FLAT);
    }

    @Test
    void zeroThresholdReactsToAnyImbalance() {
        OrderBookImbalanceSignal signal = new OrderBookImbalanceSignal(0, 1);

        assertThat(signal.get(6, 5)).isEqualTo(UP);
        assertThat(signal.get(5, 6)).isEqualTo(DOWN);
        assertThat(signal.get(5, 5)).isEqualTo(FLAT);
    }

    @Test
    void unitThresholdIsNeverCrossed() {
        OrderBookImbalanceSignal signal = new OrderBookImbalanceSignal(1, 1);

        assertThat(signal.get(100, 0)).isEqualTo(FLAT);
        assertThat(signal.get(0, 100)).isEqualTo(FLAT);
        assertThat(signal.get(10, 1)).isEqualTo(FLAT);
    }

    @Test
    void rejectsNonPositiveDenominator() {
        assertThatThrownBy(() -> new OrderBookImbalanceSignal(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderBookImbalanceSignal(1, -2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNumeratorOutOfRange() {
        assertThatThrownBy(() -> new OrderBookImbalanceSignal(-1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderBookImbalanceSignal(3, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsBoundaryThresholds() {
        new OrderBookImbalanceSignal(0, 2);
        new OrderBookImbalanceSignal(2, 2);
    }

    @Test
    void usesLongArithmeticOnLargeVolumes() {
        OrderBookImbalanceSignal signal = new OrderBookImbalanceSignal(1, 2);

        assertThat(signal.get(3_000_000_000L, 0)).isEqualTo(UP);
        assertThat(signal.get(0, 3_000_000_000L)).isEqualTo(DOWN);
    }
}
