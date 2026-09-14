# hft-learning-java

Low-latency market-data pipeline in Java 25

Works with 12Gb NASDAQ TotalView-ITCH 5.0 
decoding → order-book reconstruction → signal → market-making strategy, on an allocation-free hot path.

## Modules and key classes

| Module        | Key classes                                                                                                                                                                                                                                                                      |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `wire`        | `Itch`, `AddOrderDecoder`, `OrderExecutedWithPriceDecoder`, `OrderReplaceDecoder` — zero-copy ITCH 5.0 decoders over `MemorySegment`                                                                                                                                             |
| `trader`      | `ItchMdWalker` (feed walker), `DefaultOrderBook` + `RestingOrderPool`, `BookSide` implementations (`TreeMapBookSide`, `Long2LongRBTreeMapBookSide`, `FixedWindowLadderBookSide`, `WindowedLadderBookSide`), `OrderBookImbalanceSignal`, `MarketMakerStrategy`, `TradingPipeline` |
| `e2e`         | `Replay`, `Pipeline`, `Tickers` — wires wire + trader into a full replay pipeline                                                                                                                                                                                                |
| `feed-replay` | `Main` — standalone ITCH file replayer                                                                                                                                                                                                                                           |
| `learning`    | `Disruptor` (from-scratch ring buffer), `SPSC` queue, JMH benchmarks for false sharing, memory order, JIT and GC                                                                                                                                                                 |

Requires JDK 25 (Gradle toolchain). Build: `./gradlew build`.

## Running tests

```bash
./gradlew test  # all tests runnable from the committed data
./gradlew jmh   # JMH benchmarks, needs async-profiler (see buildSrc/.../hft.jmh-conventions.gradle)
```

## What the tests cover

- **`wire`** — per-message decoder tests (add / execute / cancel / delete / replace / trade /
  stock directory / system event) plus `ItchFeedIntegrityWalkTest`, which walks the real feed
  and asserts every wire length matches the decoder constants.
- **`trader`** — `BookSide` conformance suite run against all four implementations,
  `DefaultOrderBookTest` / `RestingOrderPoolTest`, `OrderBookImbalanceSignalTest`,
  `MarketMakerStrategyTest`, `ItchMdWalkerTest`.
- **`e2e`** — `T2TResponseTimeTest.peak5s`: real-time paced tick-to-trade replay, HdrHistogram percentiles.

## Committed market data

`data/01302020.NASDAQ_ITCH50.5mb.itch` (5 MB prefix of the 2020-01-30 NASDAQ feed) and
`data/AMD/01302020.peak_{5s,1m,5m}.itch` (AMD-only windows) are committed so the suite runs out
of the box; `scripts/itch/` downloads and trims a fresh sample. Tests needing larger captures
or long wall-clock runs carry a `@Disabled` reason:

| Test                                                         | Needs                                                                            |
|--------------------------------------------------------------|----------------------------------------------------------------------------------|
| `e2e/AllocationProofTest`                                    | full `01302020.NASDAQ_ITCH50.itch` (~12 GB) — proves the walk is allocation-free |
| `e2e/PipelineIntegrityTest`                                  | `data/AMD/01302020.all.itch` (~71 MB)                                            |
| `e2e/utils/MarketStatsReport`, `e2e/utils/ExtractLocateData` | full feed (~12 GB)                                                               |
| `e2e/T2TResponseTimeTest` `peakMinute`, `peak5Min`           | nothing extra — real-time paced replay runs 1 / 5 min                            |
| `trader/DefaultLobsterOrderBookTest`                         | LOBSTER MSFT sample (~198 MB) — book rebuilt against a reference                 |
