package com.tisov.denis.trader.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderIntentTest {

    @Test
    void asLimitSetsFieldsAndLimitType() {
        OrderIntent intent = new OrderIntent()
                .asLimit(11L, 5L, 7, Side.BID, 100_0000, 50);

        assertThat(intent.timestamp()).isEqualTo(11L);
        assertThat(intent.clientOrderId()).isEqualTo(5L);
        assertThat(intent.symbolId()).isEqualTo(7);
        assertThat(intent.side()).isEqualTo(Side.BID);
        assertThat(intent.orderType()).isEqualTo(OrderType.LIMIT);
        assertThat(intent.price()).isEqualTo(100_0000);
        assertThat(intent.quantity()).isEqualTo(50);
    }

    @Test
    void reuseOverwritesPreviousValues() {
        OrderIntent intent = new OrderIntent();
        intent.asLimit(1L, 1L, 1, Side.BID, 10, 10);

        intent.asLimit(2L, 2L, 2, Side.ASK, 20, 20);

        assertThat(intent.side()).isEqualTo(Side.ASK);
        assertThat(intent.price()).isEqualTo(20);
        assertThat(intent.clientOrderId()).isEqualTo(2L);
    }

}
