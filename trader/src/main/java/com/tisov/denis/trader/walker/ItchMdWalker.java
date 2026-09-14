package com.tisov.denis.trader.walker;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import com.tisov.denis.wire.itch.Itch;
import com.tisov.denis.wire.itch.decoder.*;

import java.lang.foreign.MemorySegment;

public final class ItchMdWalker {

    private final AddOrderDecoder addOrderDecoder = new AddOrderDecoder();
    private final AddOrderWithMpidDecoder addOrderMpidDecoder = new AddOrderWithMpidDecoder();
    private final OrderExecutedDecoder orderExecutedDecoder = new OrderExecutedDecoder();
    private final OrderExecutedWithPriceDecoder orderExecutedWithPriceDecoder = new OrderExecutedWithPriceDecoder();
    private final OrderCancelDecoder orderCancelDecoder = new OrderCancelDecoder();
    private final OrderDeleteDecoder orderDeleteDecoder = new OrderDeleteDecoder();
    private final OrderReplaceDecoder orderReplaceDecoder = new OrderReplaceDecoder();
    private final MdEvent event = new MdEvent();

    private final MdEventSink mdEventSink;

    public ItchMdWalker(MdEventSink mdEventSink) {
        this.mdEventSink = mdEventSink;
    }

    public long walk(MemorySegment memorySegment) {
        long size = memorySegment.byteSize();
        long offset = 0L;
        long messages = 0L;
        while (offset + 2 < size) {
            long msgOffset = offset + 2;
            char type = Itch.readChar(memorySegment, msgOffset);
            int len = Itch.messageLength(type);
            assert len != 0 : "ITCH type length unknown: type=%s wire length=%d".formatted(type, Itch.readU16BE(memorySegment, offset));

            switch (type) {
                case Itch.ADD_ORDER -> mdEventSink.onEvent(addOrder(memorySegment, msgOffset));
                case Itch.ADD_ORDER_MPID -> mdEventSink.onEvent(addOrderWithMpid(memorySegment, msgOffset));
                case Itch.ORDER_EXECUTED -> mdEventSink.onEvent(orderExecuted(memorySegment, msgOffset));
                case Itch.ORDER_EXECUTED_PRICE -> mdEventSink.onEvent(orderExecutedWithPrice(memorySegment, msgOffset));
                case Itch.ORDER_CANCEL -> mdEventSink.onEvent(orderCancel(memorySegment, msgOffset));
                case Itch.ORDER_DELETE -> mdEventSink.onEvent(orderDelete(memorySegment, msgOffset));
                case Itch.ORDER_REPLACE -> mdEventSink.onEvent(orderReplace(memorySegment, msgOffset));
            }
            offset = offset + 2 + len;
            ++messages;
        }
        return messages;
    }

    private MdEvent addOrder(MemorySegment memorySegment, long msgOffset) {
        this.addOrderDecoder.wrap(memorySegment, msgOffset);
        return this.event.asAdd(
                addOrderDecoder.timestamp(),
                addOrderDecoder.stockLocate(),
                addOrderDecoder.stock(),
                addOrderDecoder.orderRef(),
                sideToByte(addOrderDecoder.side()),
                addOrderDecoder.shares(),
                addOrderDecoder.price()
        );
    }

    private MdEvent addOrderWithMpid(MemorySegment memorySegment, long msgOffset) {
        this.addOrderMpidDecoder.wrap(memorySegment, msgOffset);
        return this.event.asAdd(
                addOrderMpidDecoder.timestamp(),
                addOrderMpidDecoder.stockLocate(),
                addOrderMpidDecoder.stock(),
                addOrderMpidDecoder.orderRef(),
                sideToByte(addOrderMpidDecoder.side()),
                addOrderMpidDecoder.shares(),
                addOrderMpidDecoder.price()
        );
    }

    private MdEvent orderExecuted(MemorySegment memorySegment, long msgOffset) {
        this.orderExecutedDecoder.wrap(memorySegment, msgOffset);
        return this.event.asExecute(
                orderExecutedDecoder.timestamp(),
                orderExecutedDecoder.stockLocate(),
                orderExecutedDecoder.orderRef(),
                orderExecutedDecoder.executedShares(),
                orderExecutedDecoder.matchNumber()
        );
    }

    private MdEvent orderExecutedWithPrice(MemorySegment memorySegment, long msgOffset) {
        this.orderExecutedWithPriceDecoder.wrap(memorySegment, msgOffset);
        return this.event.asExecuteWithPrice(
                orderExecutedWithPriceDecoder.timestamp(),
                orderExecutedWithPriceDecoder.stockLocate(),
                orderExecutedWithPriceDecoder.orderRef(),
                orderExecutedWithPriceDecoder.executedShares(),
                orderExecutedWithPriceDecoder.matchNumber(),
                orderExecutedWithPriceDecoder.price()

        );
    }

    private MdEvent orderCancel(MemorySegment memorySegment, long msgOffset) {
        this.orderCancelDecoder.wrap(memorySegment, msgOffset);
        return this.event.asCancel(
                orderCancelDecoder.timestamp(),
                orderCancelDecoder.stockLocate(),
                orderCancelDecoder.orderRef(),
                orderCancelDecoder.cancelledShares()
        );
    }

    private MdEvent orderDelete(MemorySegment memorySegment, long msgOffset) {
        this.orderDeleteDecoder.wrap(memorySegment, msgOffset);
        return this.event.asDelete(
                orderDeleteDecoder.timestamp(),
                orderDeleteDecoder.stockLocate(),
                orderDeleteDecoder.orderRef()
        );
    }

    private MdEvent orderReplace(MemorySegment memorySegment, long msgOffset) {
        this.orderReplaceDecoder.wrap(memorySegment, msgOffset);
        return this.event.asReplace(
                orderReplaceDecoder.timestamp(),
                orderReplaceDecoder.stockLocate(),
                orderReplaceDecoder.originalOrderRef(),
                orderReplaceDecoder.newOrderRef(),
                orderReplaceDecoder.shares(),
                orderReplaceDecoder.price()
        );
    }

    private static byte sideToByte(char side) {
        return side == 'B' ? Side.BID : Side.ASK;
    }

}
