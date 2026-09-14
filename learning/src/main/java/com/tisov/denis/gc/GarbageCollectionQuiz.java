package com.tisov.denis.gc;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class GarbageCollectionQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "gc";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. На какие три оси раскладывается выбор GC (классический trade-off)?",
                List.of(
                    "a) latency / consistency / availability",
                    "b) throughput / pause time / footprint (CPU + memory overhead)",
                    "c) heap size / class count / thread count",
                    "d) young / old / metaspace"
                ),
                Set.of("b"),
                "Классика: throughput vs pause vs footprint — pick-2. ZGC/Shenandoah жертвуют throughput и footprint ради pause. Parallel — наоборот."
            ),
            new Question(
                "2. Какие из этих утверждений про G1 верны? (несколько)",
                List.of(
                    "a) Generational, регионы equal-size 1–32 MB",
                    "b) Pause time — soft goal через -XX:MaxGCPauseMillis, не гарантия",
                    "c) Sub-ms STW паузы независимо от heap size",
                    "d) Использует SATB (snapshot-at-the-beginning) write barrier для concurrent marking",
                    "e) Mixed GC паузы могут расти с размером old gen"
                ),
                Set.of("a", "b", "d", "e"),
                "G1 — generational, region-based, SATB barrier, soft pause goal. Sub-ms — это про ZGC, не G1 (mixed GC у G1 даёт 50–200 ms на больших heap)."
            ),
            new Question(
                "3. На каком механизме построен ZGC для concurrent compaction?",
                List.of(
                    "a) Brooks pointer — дополнительное слово в header объекта",
                    "b) Colored pointers — метаданные в неиспользуемых битах указателя + load barrier",
                    "c) Read-only heap snapshots + copy-on-write",
                    "d) Card table + remembered sets"
                ),
                Set.of("b"),
                "ZGC: colored pointers (4 цвета: marked0/marked1/remapped/finalizable) + load barrier на каждом oop read. Brooks pointer — это Shenandoah (до JDK 13)."
            ),
            new Question(
                "4. Почему STW-паузы ZGC не зависят от размера heap?",
                List.of(
                    "a) STW работает только на root scan (стек тредов + GC roots), O(roots), не O(live)",
                    "b) Mark и relocation выполняются concurrent с приложением через load barrier",
                    "c) ZGC вообще не делает STW — полностью concurrent",
                    "d) Heap разбит на маленькие regions и собирается по одному"
                ),
                Set.of("a", "b"),
                "STW = только короткий root scan на флипе цвета. Всё остальное (mark, relocation, remap) — concurrent. Поэтому одинаковые sub-ms паузы на 8 GB и 16 TB."
            ),
            new Question(
                "5. Что изменилось в Generational ZGC (JEP 439, JDK 21)?",
                List.of(
                    "a) Появились young/old generations — short-lived объекты собираются дешёвым young GC",
                    "b) ZGC начал использовать compressed OOPs",
                    "c) Single-gen стагнировал на high alloc rate (mark стоил O(live set)), generational это решает",
                    "d) Стал дефолтным в JDK 24",
                    "e) Убрал load barrier"
                ),
                Set.of("a", "c", "d"),
                "Generational ZGC: young/old split, weak generational hypothesis работает (~95% объектов умирают молодыми). Default в JDK 24. Compressed OOPs — отдельная фича (JDK 22), load barrier остаётся."
            ),
            new Question(
                "6. Чем Shenandoah отличается от ZGC? (несколько)",
                List.of(
                    "a) STW <10 ms vs sub-ms у ZGC",
                    "b) Forwarding через Brooks pointer (до JDK 13) / load-ref barrier — vs colored pointers ZGC",
                    "c) Generational stable, дефолт с JDK 21",
                    "d) Heap upper ~4 TB vs ~16 TB у ZGC",
                    "e) Vendor: Red Hat / OpenJDK"
                ),
                Set.of("a", "b", "d", "e"),
                "Shenandoah: Brooks/load-ref barrier, <10 ms STW (а не sub-ms), heap до ~4 TB, Red Hat. Generational Shenandoah — experimental в JDK 24, не stable."
            ),
            new Question(
                "7. Для какого профиля нагрузки выбрать какой GC?",
                List.of(
                    "a) High-throughput batch, p99 pause не критичен → Parallel GC",
                    "b) Low-latency trading control plane, heap 64 GB, p99 < 10 ms → Generational ZGC",
                    "c) Web microservice, heap 8 GB, p99 ~100 ms OK → G1",
                    "d) HFT hot path, zero GC pause обязательно → Generational ZGC",
                    "e) HFT hot path, zero GC pause обязательно → Epsilon GC + pre-allocated arenas + zero alloc"
                ),
                Set.of("a", "b", "c", "e"),
                "На HFT hot path даже ZGC даёт ~10–15% CPU overhead через load barrier. Стандарт: Epsilon (no-op) + zero alloc steady state + restart перед OOM."
            ),
            new Question(
                "8. Зачем -XX:+AlwaysPreTouch?",
                List.of(
                    "a) Заставляет JVM коснуться (touch) каждой страницы heap при старте — page fault случается до запуска, не в runtime",
                    "b) Снижает jitter в steady state (нет lazy page allocation на горячем пути)",
                    "c) Ускоряет startup",
                    "d) Не нужен для короткоживущих процессов"
                ),
                Set.of("a", "b", "d"),
                "PreTouch замедляет startup (touch всех страниц), но убирает page fault stalls в runtime. Для batch / короткоживущих — лишнее."
            ),
            new Question(
                """
                --- Лог ---
                [gc          ] GC(42) Pause Young (Mixed) (G1 Evacuation Pause) 1024M->768M(2048M) 187.345ms
                [safepoint   ] Safepoint "G1CollectForAllocation",
                  Reaching safepoint:      72,011 ns
                  At safepoint:       187,000,000 ns""",
                "9. Что говорит этот лог?",
                List.of(
                    "a) Mixed GC длится 187 ms — soft pause goal не соблюдён",
                    "b) TTSP здоровый (~72 µs), проблема в самой паузе GC",
                    "c) Корень проблемы — counted loop без safepoint poll",
                    "d) На таких паузах G1 не годится для p99 < 50 ms — нужен ZGC"
                ),
                Set.of("a", "b", "d"),
                "187 ms — это At safepoint (mixed GC), TTSP в порядке. Mixed GC масштабируется с old gen → классический повод уйти на ZGC если требуется sub-10ms p99."
            ),
            new Question(
                "10. Как корректно мерить allocation rate в Java? (несколько)",
                List.of(
                    "a) JFR события jdk.ObjectAllocationInNewTLAB / jdk.ObjectAllocationOutsideTLAB",
                    "b) async-profiler -e alloc (alloc flame graph)",
                    "c) JMH с профилем `-prof gc` (вернёт ·gc.alloc.rate.norm в B/op)",
                    "d) Считать new'ы вручную в коде",
                    "e) System.gc() и смотреть delta"
                ),
                Set.of("a", "b", "c"),
                "Стандарт: JFR (production-safe), async-profiler (sampling, low overhead), JMH -prof gc (B/op для бенчмарка). System.gc() ничего не меряет."
            ),
            new Question(
                "11. Целевые allocation rate для HFT?",
                List.of(
                    "a) Hot path: 0 B/op (steady state — никаких new)",
                    "b) Hot path: < 100 MB/s допустимо",
                    "c) Control plane: < 1 GB/s",
                    "d) Single-gen ZGC уверенно держит >5 GB/s",
                    "e) Generational ZGC держит >5 GB/s — generational hypothesis работает"
                ),
                Set.of("a", "c", "e"),
                "Hot path = строго 0 B/op (проверяется async-profiler --alloc). Single-gen ZGC начинал стагнировать на ~1 GB/s, generational справляется."
            ),
            new Question(
                "12. Что такое humongous object в G1 и почему это проблема?",
                List.of(
                    "a) Объект размером ≥ ½ региона — аллоцируется напрямую в old gen",
                    "b) Объект, переживший >15 minor GC циклов",
                    "c) До JDK 11 собирался только при Full GC — источник долгих пауз",
                    "d) Может фрагментировать heap (один объект = один регион, остатки регионов теряются)"
                ),
                Set.of("a", "c", "d"),
                "Humongous = ≥ ½ region. До JDK 11 — collected только на Full GC. Фрагментация regions — реальная боль G1 на массивах/буферах."
            ),
            new Question(
                "13. Топ-3 источника длинных STW по GC — сопоставь правильно. (несколько)",
                List.of(
                    "a) G1 → Mixed GC растёт с размером old gen",
                    "b) ZGC → root scan с гигантскими стеками тредов",
                    "c) Shenandoah → init/final mark на широком root set, update-refs final phase",
                    "d) Generational ZGC → mark всего heap на каждом цикле",
                    "e) G1 → load barrier на каждом read"
                ),
                Set.of("a", "b", "c"),
                "G1 — mixed GC + Full GC. ZGC — root scan (sub-ms by design в остальном). Shenandoah — init/final mark + update-refs. Load barrier — это ZGC/Shenandoah (overhead, не STW), а Generational ZGC не делает mark всего heap (в этом и смысл)."
            ),
            new Question(
                "14. Какие knobs реально уменьшают p99 pause на ZGC? (несколько)",
                List.of(
                    "a) Включить +ZGenerational (cut young GC cost для short-lived alloc)",
                    "b) +AlwaysPreTouch + huge pages (убрать page-fault stalls и TLB miss)",
                    "c) ↑ -XX:ConcGCThreads — шире concurrent работа",
                    "d) Уменьшить root set: меньше потоков, короче стеки",
                    "e) ↓ -Xmx — меньше heap → меньше mark, как в G1"
                ),
                Set.of("a", "b", "c", "d"),
                "У ZGC pause = O(roots), не O(heap). Уменьшение Xmx не помогает. Помогает: generational, pretouch + huge pages, больше concurrent threads, меньше тредов/стеков."
            ),
            new Question(
                "15. Какие JEP стоит назвать в контексте \"что нового в GC за последние LTS\"? (несколько)",
                List.of(
                    "a) JEP 333 — ZGC experimental (JDK 11)",
                    "b) JEP 439 — Generational ZGC (JDK 21)",
                    "c) JEP 450 — Compact Object Headers / Project Lilliput (JDK 24)",
                    "d) JEP 374 — Disable and Deprecate Biased Locking (JDK 15)",
                    "e) JEP 444 — Virtual Threads (JDK 21)"
                ),
                Set.of("a", "b", "c"),
                "JEP про GC: 333 (ZGC experimental), 439 (Generational ZGC), 450 (Lilliput — снижает memory overhead для всех GC). JEP 374 — locking, JEP 444 — Loom, к GC не относятся."
            ),
            new Question(
                "16. Compressed OOPs и современный ZGC — что верно?",
                List.of(
                    "a) ZGC до JDK 21 не поддерживал compressed OOPs → footprint больше vs G1 на heap < 32 GB",
                    "b) ZGC всегда поддерживал compressed OOPs",
                    "c) С JDK 22 ZGC поддерживает compressed OOPs",
                    "d) Compressed OOPs работают только на heap ≤ 32 GB (4 байта на ref)"
                ),
                Set.of("a", "c", "d"),
                "Исторически ZGC = +30–50% footprint vs G1 на маленьких heap. С JDK 22 поправлено. Compressed OOPs — ≤32 GB фундаментально."
            ),
            new Question(
                "17. Команда для GC-логов и анализа — что верно? (несколько)",
                List.of(
                    "a) Современный синтаксис: -Xlog:gc*:file=gc.log:time,uptime,level,tags",
                    "b) Старый синтаксис -XX:+PrintGCDetails — deprecated, не использовать на JDK 11+",
                    "c) GCEasy / GCViewer открывают raw GC log",
                    "d) Извлечь p99 из лога можно: grep -oP 'Pause.*\\K[0-9.]+ms' | sort -n | awk percentile"
                ),
                Set.of("a", "b", "c", "d"),
                "Современный синтаксис с JDK 9 (-Xlog). PrintGCDetails — legacy. GCEasy (web) и GCViewer (desktop) понимают raw log. Парсер на grep+awk — стандартный приём."
            )
        );
    }
}
