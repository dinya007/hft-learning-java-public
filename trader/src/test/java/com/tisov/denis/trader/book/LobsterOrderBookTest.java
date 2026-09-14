package com.tisov.denis.trader.book;

import com.tisov.denis.trader.domain.MdEvent;
import com.tisov.denis.trader.domain.Side;
import com.tisov.denis.trader.sink.MdEventSink;
import it.unimi.dsi.fastutil.Pair;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class LobsterOrderBookTest {

    private static final int LEVELS = 10;
    private static final int PREFIX = 4_000;
    private static final int CAPACITY = 1 << 16;

    private static final Path DIR = Path.of("..").toAbsolutePath()
            .resolve("data/lobster/LOBSTER_SampleFile_MSFT_2012-06-21_" + LEVELS);
    private static final Path MESSAGES = DIR.resolve("MSFT_2012-06-21_34200000_57600000_message_" + LEVELS + ".csv");
    private static final Path ORDERBOOK = DIR.resolve("MSFT_2012-06-21_34200000_57600000_orderbook_" + LEVELS + ".csv");

    private static final int SUBMIT = 1, CANCEL = 2, DELETE = 3, EXECUTE = 4;

    public abstract Pair<OrderBook, MdEventSink> orderBook(int capacity);

    private final MdEvent ev = new MdEvent();
    private final Map<Long, Long> remaining = new HashMap<>();
    private final Map<Long, Long> seedAsk = new HashMap<>();
    private final Map<Long, Long> seedBid = new HashMap<>();
    private long seedSeq = 0;

    @Test
    void reconstructsTopOfBook() throws IOException {
        Pair<OrderBook, MdEventSink> pair = orderBook(CAPACITY);
        OrderBook book = pair.left();
        MdEventSink sink = pair.right();

        try (BufferedReader messages = Files.newBufferedReader(MESSAGES);
             BufferedReader orderbook = Files.newBufferedReader(ORDERBOOK)) {

            seed(sink, orderbook.readLine().split(","));
            messages.readLine();

            for (int row = 1; row <= PREFIX; row++) {
                String[] m = messages.readLine().split(",");
                String[] ob = orderbook.readLine().split(",");
                apply(sink, parseInt(m[1]), parseLong(m[2]), parseLong(m[3]), parseLong(m[4]),
                        parseInt(m[5]) == 1 ? Side.BID : Side.ASK);
                assertL1(book, ob, row);
            }
        }
    }

    private void seed(MdEventSink sink, String[] row) {
        for (int lvl = 0; lvl < LEVELS; lvl++) {
            long askPrice = parseLong(row[4 * lvl]), askShares = parseLong(row[4 * lvl + 1]);
            long bidPrice = parseLong(row[4 * lvl + 2]), bidShares = parseLong(row[4 * lvl + 3]);
            if (askShares > 0) {
                seedAsk.put(askPrice, add(sink, Side.ASK, askPrice, askShares));
            }
            if (bidShares > 0) {
                seedBid.put(bidPrice, add(sink, Side.BID, bidPrice, bidShares));
            }
        }
    }

    private void apply(MdEventSink sink, int type, long oid, long size, long price, byte side) {
        if (type == SUBMIT) {
            add(sink, side, price, size, oid);
        } else if (type == CANCEL || type == DELETE || type == EXECUTE) {
            if (remaining.containsKey(oid)) {
                reduce(sink, oid, size);
            } else {
                Map<Long, Long> seed = side == Side.ASK ? seedAsk : seedBid;
                Long ref = seed.get(price);
                if (ref != null && reduce(sink, ref, size)) seed.remove(price);
            }
        }
    }

    private void add(MdEventSink sink, byte side, long price, long shares, long ref) {
        remaining.put(ref, shares);
        sink.onEvent(ev.asAdd(0L, 0, 0L, ref, side, shares, price));
    }

    private long add(MdEventSink sink, byte side, long price, long shares) {
        long ref = --seedSeq;
        add(sink, side, price, shares, ref);
        return ref;
    }

    private boolean reduce(MdEventSink sink, long ref, long size) {
        long left = remaining.get(ref) - size;
        sink.onEvent(ev.asCancel(0L, 0, ref, size));
        if (left <= 0) {
            remaining.remove(ref);
            return true;
        }
        remaining.put(ref, left);
        return false;
    }

    private void assertL1(OrderBook book, String[] ob, int row) {
        long askPrice = parseLong(ob[0]), askShares = parseLong(ob[1]);
        long bidPrice = parseLong(ob[2]), bidShares = parseLong(ob[3]);
        assertThat(book.bestAsk()).as("bestAsk @row %d", row).isEqualTo(askShares > 0 ? askPrice : Long.MIN_VALUE);
        assertThat(book.bestBid()).as("bestBid @row %d", row).isEqualTo(bidShares > 0 ? bidPrice : Long.MIN_VALUE);
        if (askShares > 0) assertThat(book.bestAskShares()).as("bestAsk shares @row %d", row).isEqualTo(askShares);
        if (bidShares > 0) assertThat(book.bestBidShares()).as("bestBid shares @row %d", row).isEqualTo(bidShares);
    }
}
