package com.tisov.denis.jit;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class SafePointQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "safepoint";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Что такое safepoint в JVM?",
                List.of(
                    "a) Точка в коде, где GC запускается принудительно",
                    "b) Точка, где JVM может остановить все треды в согласованном состоянии",
                    "c) Системный вызов в ядро ОС для синхронизации",
                    "d) Барьер памяти аналогичный StoreLoad"
                ),
                Set.of("b"),
                "Safepoint — точка где JVM гарантированно может остановить ВСЕ треды для согласованного состояния (GC, deopt, dump, JFR)."
            ),
            new Question(
                "2. Какие из событий триггерят safepoint? (несколько)",
                List.of(
                    "a) Minor GC / G1CollectForAllocation",
                    "b) Volatile write в long",
                    "c) Thread.getAllStackTraces() / ThreadDump",
                    "d) Deoptimization",
                    "e) Lock-free CAS на AtomicLong"
                ),
                Set.of("a", "c", "d"),
                "GC, ThreadDump, Deoptimize, Cleanup, FindDeadlocks, ICBufferFull — всё это safepoints. Volatile и CAS работают без safepoint."
            ),
            new Question(
                "3. Почему «GC pause» != «JVM pause»?",
                List.of(
                    "a) GC-логи показывают только At safepoint, не учитывая TTSP",
                    "b) GC пауза измеряется в µs, а JVM в ms — разные единицы",
                    "c) GC параллельный, а JVM пауза однопоточная",
                    "d) JVM pause включает TTSP + At safepoint + Leaving — user-visible пауза"
                ),
                Set.of("a", "d"),
                "Реальная пауза = TTSP + At + Leaving. GC меряет только свою часть (At). Если Reaching > At — проблема не в GC, а в TTSP."
            ),
            new Question(
                "4. Что означает поле `Reaching safepoint` в -Xlog:safepoint?",
                List.of(
                    "a) Сколько работала операция в safepoint (GC/deopt)",
                    "b) TTSP — время ожидания пока ВСЕ треды дойдут до safepoint poll",
                    "c) Время на возобновление тредов после safepoint",
                    "d) Период между двумя соседними safepoints"
                ),
                Set.of("b"),
                "Reaching safepoint = TTSP. Это ожидание пока все runnable-треды доползут до ближайшего poll-points."
            ),
            new Question(
                """
                --- Лог №1 ---
                Safepoint "G1CollectForAllocation",
                  Reaching safepoint:  45,000,000 ns
                  At safepoint:         2,000,000 ns
                  Leaving safepoint:        3,000 ns
                  Threads: 1 runnable, 12 total""",
                "5. Что говорит этот лог?",
                List.of(
                    "a) GC долгий — надо тюнить G1",
                    "b) TTSP ~45 ms, GC всего ~2 ms — корень в долгом достижении safepoint",
                    "c) Один тред застрял (видимо в counted loop / JNI / park)",
                    "d) Утечка памяти в куче"
                ),
                Set.of("b", "c"),
                "Reaching (45ms) >> At (2ms) → проблема не в GC. 1 runnable тред — кандидат: counted loop без poll, долгий JNI, monitor inflation."
            ),
            new Question(
                """
                --- Лог №2 ---
                Safepoint "Deoptimize",
                  Reaching safepoint:  58,340 ns
                  At safepoint:        21,500 ns""",
                "6. Это GC-пауза?",
                List.of(
                    "a) Да, любой safepoint — это GC",
                    "b) Нет, это JIT deoptimization (откат к интерпретатору)",
                    "c) Это thread dump",
                    "d) Это biased lock revocation"
                ),
                Set.of("b"),
                "Имя safepoint = \"Deoptimize\". Причина: нарушение спекуляции JIT (новый subtype, null check, unstable_if)."
            ),
            new Question(
                "7. Почему counted `int`-цикл на JDK 15 убивает TTSP?",
                List.of(
                    "a) Внутри цикла происходят аллокации",
                    "b) C2 не вставляет safepoint poll внутри counted int-loop — поток доходит до poll только после завершения всего цикла",
                    "c) JIT компилирует цикл медленно",
                    "d) Inline cache переполняется"
                ),
                Set.of("b"),
                "До JDK 16 C2 опускал poll в counted int-loop как оптимизацию. TTSP = время всего цикла (сотни ms / секунды)."
            ),
            new Question(
                "8. Как ведёт себя поток в JNI при запросе safepoint?",
                List.of(
                    "a) JVM ждёт его как обычный Java-тред — TTSP растёт",
                    "b) Поток в JNI считается \"at safepoint\" автоматически, JVM не ждёт",
                    "c) JVM убивает JNI-поток",
                    "d) Задержка возникает при ВОЗВРАТЕ из JNI: тред видит флаг и блокируется"
                ),
                Set.of("b", "d"),
                "JNI-тред автоматом «at safepoint» — JVM его не ждёт. Но при возврате тред проверит флаг и заблокируется."
            ),
            new Question(
                "9. Тред в LockSupport.park() / Object.wait() — что с safepoint?",
                List.of(
                    "a) Считается заблокированным = at safepoint, не задерживает TTSP",
                    "b) Должен быть разбужен чтобы дойти до poll — задерживает TTSP",
                    "c) Игнорируется JVM навсегда",
                    "d) Автоматически переходит в runnable при запросе safepoint"
                ),
                Set.of("a"),
                "Парк/wait — состояние WAITING, тред уже не выполняет байткод → считается at safepoint. JVM его не ждёт."
            ),
            new Question(
                "10. Что делает loop strip mining (JDK 16+)?",
                List.of(
                    "a) Разворачивает цикл целиком (unrolling)",
                    "b) Разбивает counted loop на внешний и внутренний: poll вставляется на back-edge внешнего, каждые N итераций",
                    "c) Отключает counted loop полностью",
                    "d) Размер strip регулируется -XX:LoopStripMiningIter"
                ),
                Set.of("b", "d"),
                "Внешний цикл с poll, внутренний — без poll. Размер: LoopStripMiningIter (дефолт 1000). TTSP падает с сотен ms до десятков µs."
            ),
            new Question(
                "11. Что насчёт флага -XX:+UseCountedLoopSafepoints?",
                List.of(
                    "a) Доступен с JDK 10, по дефолту выключен до JDK 16",
                    "b) На JDK 11 в продакшене HFT — включать вручную",
                    "c) Удаляет все safepoints из программы",
                    "d) Работает только с G1"
                ),
                Set.of("a", "b"),
                "Backport с JDK 10. На legacy кластерах (JDK 11) обязательно включать. С JDK 16 — дефолт."
            ),
            new Question(
                "12. Что НЕ должно быть на hot path в HFT (минимизация safepoints)?",
                List.of(
                    "a) Аллокации новых объектов",
                    "b) Чтение поля из заранее созданного объекта",
                    "c) System.gc() / Thread.getAllStackTraces() / Class.forName()",
                    "d) Полиморфные вызовы 3+ типов (мегаморфные call sites → deopt)"
                ),
                Set.of("a", "c", "d"),
                "Аллокации → GC safepoint. Явные триггеры (gc/dump/forName) → их safepoints. Мегаморфные сайты → deopt. Чтение поля само по себе безопасно."
            ),
            new Question(
                """
                --- Лог №3 ---
                Safepoint "G1CollectFull",
                  Reaching safepoint:    67,623 ns
                  At safepoint:       4,847,917 ns
                  Total:              4,918,587 ns
                  Threads: 1 runnable, 12 total""",
                "13. На что смотреть в этом логе?",
                List.of(
                    "a) TTSP здоровый (~67 µs), пауза — это Full GC (~4.85 ms)",
                    "b) Корень проблемы — TTSP",
                    "c) Full GC = тревога: System.gc() / нехватка кучи",
                    "d) 1 runnable значит deadlock"
                ),
                Set.of("a", "c"),
                "TTSP в норме, At = ~5 ms Full GC — это плохо само по себе (full collection), но не TTSP-проблема. 1 runnable — нормальная цифра (worker-тред)."
            )
        );
    }
}
