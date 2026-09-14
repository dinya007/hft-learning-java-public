package com.tisov.denis.jit;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class JitCompilationQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "jit-compilation";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Каков путь горячего метода через тиры JIT?",
                List.of(
                    "a) 0 → 1 → 2 → 3 → 4",
                    "b) 0 → 3 → 4",
                    "c) 0 → 4 напрямую",
                    "d) 3 → 4, интерпретатор не задействован"
                ),
                Set.of("b"),
                "Типичный путь: tier 0 (интерпретатор, профилирование) → tier 3 (C1 с полным профилем) → tier 4 (C2, спекулятивные оптимизации). Tier 1/2 — для методов, которые C2 не берёт."
            ),
            new Question(
                "2. Что делает C2 на tier 4, чего не делает C1? (несколько)",
                List.of(
                    "a) Спекулятивный инлайнинг виртуальных вызовов",
                    "b) Компилирует байткод в нативный код",
                    "c) Использует FreqInlineSize=325 для горячих методов",
                    "d) Вставляет safepoint polls в counted loops (JDK 16+)"
                ),
                Set.of("a", "c"),
                "C2 использует профиль от C1 и делает спекулятивный инлайнинг (с guard), применяет FreqInlineSize=325. C1 тоже компилирует байткод (b), loop strip mining — отдельный механизм не связанный с тиром (d)."
            ),
            new Question(
                "3. Что означает символ `%` в строке лога -Xlog:jit+compilation?",
                List.of(
                    "a) Метод скомпилирован на tier 4 (C2)",
                    "b) OSR — On-Stack Replacement: компиляция цикла прямо во время его выполнения",
                    "c) Метод является native",
                    "d) Компиляция заблокирована (blocked)"
                ),
                Set.of("b"),
                "% = OSR. JIT обнаружил горячий цикл во время его выполнения и скомпилировал без ожидания следующего вызова метода. Точка входа в OSR-код — bytecode offset @ N."
            ),
            new Question(
                "4. В чём отличие OSR-компиляции от обычной?",
                List.of(
                    "a) OSR вступает в силу прямо в текущем вызове метода, не дожидаясь следующего",
                    "b) OSR всегда на tier 4 (C2)",
                    "c) Обычная компиляция нужна для следующих вызовов, OSR — для текущего",
                    "d) OSR невозможен на tier 3"
                ),
                Set.of("a", "c"),
                "OSR заменяет интерпретируемый код прямо в текущем исполнении цикла (@ bytecode offset). Обычная компиляция вступит в силу при следующем вызове метода. OSR возможен на tier 3 и 4."
            ),
            new Question(
                """
                --- Лог ---
                15 %  3   Foo::hotMethod @ 10   made not entrant: OSR invalidation of lower level
                17 %  4   Foo::hotMethod @ 10   made not entrant: uncommon trap""",
                "5. Что произошло с методом?",
                List.of(
                    "a) ID 15 (C1) вытеснен C2 — норма при tier upgrade",
                    "b) ID 17 (C2) деоптимизирован — спекулятивный guard сработал",
                    "c) Оба события — ошибки JVM",
                    "d) `uncommon trap` означает переполнение code cache"
                ),
                Set.of("a", "b"),
                "OSR invalidation of lower level = C1 вытеснен C2, это норма. uncommon trap на C2 = guard (type check / null check / branch prediction) нарушен → deopt → откат к интерпретатору."
            ),
            new Question(
                "6. Какие причины deoptimization возникают на hot path в HFT? (несколько)",
                List.of(
                    "a) class_check — новый subtype нарушил монорморфный inline cache",
                    "b) div0_check — деление на ноль",
                    "c) unstable_if — branch стал браться иначе чем в профиле",
                    "d) null_check — JIT убрал null-check, пришёл null",
                    "e) loop_limit_check — нарушена граница цикла"
                ),
                Set.of("a", "c", "d"),
                "class_check, unstable_if, null_check — самые частые на hot path. div0_check и loop_limit_check редки и обычно не на hot path."
            ),
            new Question(
                "7. Почему C1 не использует FreqInlineSize=325?",
                List.of(
                    "a) C1 медленнее C2 и не может позволить дорогой анализ",
                    "b) Задача C1 — быстро скомпилировать и собрать профиль для C2, а не агрессивно оптимизировать",
                    "c) FreqInlineSize применяется только в JDK 17+",
                    "d) C1 всегда инлайнит всё что можно"
                ),
                Set.of("b"),
                "C1 — быстрый компилятор-профилировщик. Агрессивный инлайнинг замедлил бы C1-компиляцию и отложил передачу управления C2. Горячий инлайнинг — задача C2, у которого уже есть готовый профиль."
            ),
            new Question(
                """
                --- Лог ---
                [debug][jit,inlining]  @ 12  bigMethod (351 bytes)  failed to inline: hot method too big
                [debug][jit,inlining]  @ 12  bigMethod (351 bytes)  failed to inline: callee is too large""",
                "8. Какая строка от C2, какая от C1?",
                List.of(
                    "a) `hot method too big` — C2, `callee is too large` — C1",
                    "b) Обе от C1",
                    "c) `callee is too large` — C2, `hot method too big` — C1",
                    "d) Обе от C2"
                ),
                Set.of("a"),
                "`callee is too large` = C1: метод > MaxInlineSize(35). `hot method too big` = C2: метод горячий, но > FreqInlineSize(325). C2 честно говорит что хотел бы заинлайнить, но размер не позволяет."
            ),
            new Question(
                "9. Мегафункция process(400 bytes) вызывается из hotLoop. Что происходит с step1/step2 внутри process?",
                List.of(
                    "a) C2 инлайнит step1/step2 внутри process, но из hotLoop они невидимы",
                    "b) C2 инлайнит step1/step2 из hotLoop через process",
                    "c) step1/step2 не компилируются вообще",
                    "d) C2 не видит step1/step2 нигде"
                ),
                Set.of("a"),
                "process компилируется отдельно — C2 инлайнит step1/step2 внутри process. Но C2 при компиляции hotLoop не заглядывает внутрь process (too large) → сквозная оптимизация через всю цепочку невозможна."
            ),
            new Question(
                "10. Как увидеть inlining-решения C2 на продакшн JVM?",
                List.of(
                    "a) -Xlog:jit+inlining=info",
                    "b) -Xlog:jit+inlining=debug",
                    "c) -XX:+PrintOptoInlining",
                    "d) -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation → JITWatch"
                ),
                Set.of("b", "d"),
                "`=debug` показывает C2 inlining в unified logging. LogCompilation+JITWatch даёт полное дерево. PrintOptoInlining — только debug-сборка JVM, на продакшн недоступен."
            ),
            new Question(
                "11. Сколько итераций цикла нужно чтобы C2 успел скомпилировать метод до смены типа?",
                List.of(
                    "a) ~1 000",
                    "b) ~10 000",
                    "c) ~200 000+",
                    "d) Достаточно 100 — C2 очень быстрый"
                ),
                Set.of("c"),
                "C2 компилирует при ~10 000–15 000 вызовов, но OSR тоже нужно время. На практике для стабильного C2 OSR нужно ~200 000+ итераций. При 1 000 итераций C2 не успевает."
            ),
            new Question(
                "12. Что делать с мегафункцией process(400 bytes) на hot path?",
                List.of(
                    "a) Увеличить -XX:FreqInlineSize=500",
                    "b) Разбить на части < 325 байт чтобы C2 мог инлайнить всю цепочку",
                    "c) Добавить final — это поможет инлайнингу",
                    "d) Ничего — C2 всё равно оптимизирует через JNI"
                ),
                Set.of("b"),
                "Правильный путь — разбить на методы < 325 байт. Тогда C2 заинлайнит всю цепочку и получит полный граф для оптимизации. Увеличение FreqInlineSize не рекомендуется — ухудшает code cache."
            ),
            new Question(
                "13. Что из перечисленного корректно описывает PrintInlining? (несколько)",
                List.of(
                    "a) Показывает дерево инлайнинга C1",
                    "b) Показывает дерево инлайнинга C2 полностью",
                    "c) Прикрепляет дерево под строкой -Xlog:jit+compilation",
                    "d) Для C2 нужен -Xlog:jit+inlining=debug или LogCompilation+JITWatch"
                ),
                Set.of("a", "c", "d"),
                "PrintInlining показывает C1-дерево и прикрепляет его к строкам compilation-лога. C2-дерево полностью через PrintInlining не видно на продакшн JVM."
            ),
            new Question(
                """
                --- Код ---
                public static void main(String[] args) {
                    while (true) {
                        processMarketData();
                    }
                }""",
                "14. Метод main() вызывается один раз и содержит цикл на миллионы итераций. Будет ли он скомпилирован?",
                List.of(
                    "a) Нет, invocation counter = 1, метод никогда не дойдёт до порога компиляции",
                    "b) Да, через back-edge counter и OSR — компиляция текущего фрейма прямо на стеке",
                    "c) Только processMarketData() будет скомпилирован, main() останется в интерпретаторе",
                    "d) Только если явно прогреть через -XX:+CompileOnly"
                ),
                Set.of("b"),
                "HotSpot ведёт два независимых счётчика на метод. Back-edge counter считает итерации цикла → при достижении порога (OnStackReplacePercentage × CompileThreshold / 100) запускается OSR-компиляция, и интерпретируемый фрейм main() заменяется на скомпилированный прямо на стеке. Это и есть why OSR существует."
            ),
            new Question(
                "15. Сколько счётчиков HotSpot ведёт на каждый метод и зачем каждый? (несколько)",
                List.of(
                    "a) Один общий счётчик, его значение делится виртуально на invocation и back-edge",
                    "b) Invocation counter — считает вызовы метода, триггерит обычную компиляцию (вступает в силу при следующем вызове)",
                    "c) Back-edge counter — считает исполнение back-edge внутри метода, триггерит OSR (замену фрейма в текущем вызове)",
                    "d) Три независимых счётчика: invocation, back-edge, deopt"
                ),
                Set.of("b", "c"),
                "Два независимых счётчика в MethodCounters: invocation для обычной компиляции, back-edge для OSR. Без back-edge counter долгоживущие методы с горячим циклом (main с while(true), batch-обработка) навсегда оставались бы в интерпретаторе — invocation counter = 1 не дотянул бы до порога."
            ),
            new Question(
                """
                --- Лог после warmup ---
                42 %  4   App::eventLoop @ 12     ← OSR на главном loop
                58 %  4   App::parseMessage @ 8   ← OSR на work-методе""",
                "16. Какая из двух OSR-компиляций — сигнал проблемы для HFT? (несколько)",
                List.of(
                    "a) Первая (eventLoop) — норма, invocation counter навсегда = 1, без OSR метод остался бы в интерпретаторе",
                    "b) Первая (eventLoop) — проблема, надо избавиться от внешнего цикла",
                    "c) Вторая (parseMessage) — сигнал: либо метод не прогрет до invocation-компиляции, либо у него внутри слишком длинный цикл",
                    "d) Вторая (parseMessage) — норма, work-методы всегда идут через OSR"
                ),
                Set.of("a", "c"),
                "OSR на главном event-loop фрейме неизбежен и нормален. Сигнал проблемы — OSR на work-методах внутри loop body: либо warmup не довёл их до invocation counter (≥10k вызовов), либо у самого work-метода есть слишком большой внутренний цикл, который триггерит OSR раньше invocation counter'а. Лечение — структурировать hot path как набор коротких методов, прогревать каждый ≥10k раз."
            )
        );
    }
}
