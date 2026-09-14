package com.tisov.denis.trader.book;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestingOrderPoolTest {

    RestingOrderPool pool = new RestingOrderPool(128);

    @Test
    void acquireSequentialThenExhaust() {
        var pool = new RestingOrderPool(128);

        for (int i = 0; i < 128; i++) {
            assertThat(pool.acquire()).isEqualTo(i);
        }

        assertThat(pool.inUse()).isEqualTo(128);
        assertThat(pool.peakInUse()).isEqualTo(128);
        assertThatThrownBy(pool::acquire).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releaseReusesLowestIndex() {
        var pool = new RestingOrderPool(128);
        for (int i = 0; i < 64; i++) {
            pool.acquire();
        }

        pool.release(10);

        assertThat(pool.acquire()).isEqualTo(10);
        assertThat(pool.acquire()).isEqualTo(64);
    }

    @Test
    void churnKeepsInUseConsistent() {
        var pool = new RestingOrderPool(64);

        for (int round = 0; round < 1_000_000; round++) {
            int a = pool.acquire();
            int b = pool.acquire();
            pool.release(a);
            pool.release(b);
        }

        assertThat(pool.inUse()).isZero();
    }

}
