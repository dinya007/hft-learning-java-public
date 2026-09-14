package com.tisov.denis.memoryorder;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class MemoryModelQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "memory-model";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Что такое VarHandle и зачем он появился в JDK 9?",
                List.of(
                    "a) Заменил sun.misc.Unsafe для атомарных операций в публичном API",
                    "b) Даёт типобезопасный доступ к полям с контролем ordering",
                    "c) Нужен только для работы с off-heap памятью",
                    "d) Полный аналог AtomicLong — без дополнительных возможностей"
                ),
                Set.of("a", "b"),
                "VarHandle = типобезопасная замена Unsafe с явными ordering-режимами. Позволяет использовать relaxed/acquire/release/volatile семантику на обычных полях без AtomicXxx обёрток."
            ),
            new Question(
                "2. Что гарантирует getAcquire / setRelease?",
                List.of(
                    "a) setRelease: всё написанное ДО видно тому, кто сделал getAcquire этого значения",
                    "b) Парная happens-before между конкретным писателем и конкретным читателем",
                    "c) Глобальный порядок всех операций (total order)",
                    "d) Аналог volatile: более дешёвый на x86, дороже на ARM"
                ),
                Set.of("a", "b", "d"),
                "acquire/release = парная HB: писатель→читатель через конкретное значение. Нет total order (в отличие от volatile/seq_cst). На x86 дешевле volatile (volatile требует StoreLoad fence)."
            ),
            new Question(
                "3. Что такое getOpaque?",
                List.of(
                    "a) Атомарный доступ без переупорядочивания с другими opaque-операциями на том же переменной",
                    "b) Гарантирует видимость без ordering-гарантий для других переменных",
                    "c) Аналог non-volatile read/write — без каких-либо гарантий",
                    "d) Полезен когда нужна только атомарность чтения/записи, но не happens-before"
                ),
                Set.of("a", "b", "d"),
                "Opaque = самый слабый ordering выше plain. Гарантирует: атомарность, coherence (глобальный порядок для этой переменной). Не гарантирует: видимость других переменных, happens-before с другими. Дешевле acquire/release."
            ),
            new Question(
                "4. Чем volatile (setVolatile/getVolatile) отличается от setRelease/getAcquire?",
                List.of(
                    "a) volatile гарантирует total order всех volatile-операций (seq_cst)",
                    "b) acquire/release — только парная HB без total order",
                    "c) На x86 volatile дороже: требует StoreLoad fence (mfence или lock addl)",
                    "d) volatile и acquire/release одинаковы на всех архитектурах"
                ),
                Set.of("a", "b", "c"),
                "volatile = seq_cst в Java: total order + StoreLoad fence. acquire/release = парная HB, на x86 бесплатны (TSO даёт LoadLoad/StoreStore/LoadStore бесплатно). d) неверно — на ARM разница огромная."
            ),
            new Question(
                "5. Как JIT компилирует setVolatile на x86?",
                List.of(
                    "a) mov + lock addl $0, -0x40(%rsp) — RMW на стек, дешевле mfence",
                    "b) mfence после каждого volatile write",
                    "c) xchg (LOCK XCHG) на каждой volatile записи",
                    "d) Обычный mov без барьера — x86 TSO достаточно"
                ),
                Set.of("a"),
                "JIT генерирует mov (запись) + lock addl $0, -0x40(%rsp). lock addl — RMW-операция на пустую ячейку стека, это full memory barrier дешевле mfence. Profiler skid может атрибутировать стоимость на следующую инструкцию."
            ),
            new Question(
                "6. Какие перестановки запрещены на x86 (TSO)? (несколько)",
                List.of(
                    "a) StoreLoad — разрешён: store может оставаться в store buffer, load уходит вперёд",
                    "b) LoadLoad — запрещён на x86",
                    "c) StoreStore — запрещён на x86",
                    "d) LoadStore — запрещён на x86"
                ),
                Set.of("b", "c", "d"),
                "x86 TSO запрещает LoadLoad, StoreStore, LoadStore перестановки. StoreLoad — единственная разрешённая: store в store buffer, последующий load к другой переменной может завершиться раньше. Именно это ломает Dekker."
            ),
            new Question(
                "7. Что такое IRIW (Independent Reads of Independent Writes)?",
                List.of(
                    "a) Два наблюдателя видят две независимые записи в разном порядке",
                    "b) Невозможен на x86 (other-multi-copy-atomic)",
                    "c) Возможен на ARM/POWER с acquire/release",
                    "d) seq_cst (volatile) запрещает IRIW на всех архитектурах"
                ),
                Set.of("a", "b", "c", "d"),
                "IRIW: T3 видит x=1 до y=1, T4 видит y=1 до x=1 — противоречие. На x86 невозможен (all cores see stores in same order). На ARM/POWER воспроизводится с acq/rel. volatile (seq_cst) навязывает total order и запрещает IRIW везде."
            ),
            new Question(
                "8. Что такое Dekker scenario и на каких архитектурах воспроизводится?",
                List.of(
                    "a) T1: x=1; r1=y; T2: y=1; r2=x → оба видят 0 — StoreLoad reordering",
                    "b) Воспроизводится на x86 с release/acquire",
                    "c) На x86 невозможен — TSO запрещает",
                    "d) Воспроизводится на всех архитектурах с acq/rel, включая x86"
                ),
                Set.of("a", "b", "d"),
                "Dekker: store попадает в store buffer, load к другой переменной обгоняет его — оба не видят чужого store. x86 допускает StoreLoad, поэтому Dekker воспроизводим. c) неверно. Нужен seq_cst (volatile) для запрета."
            ),
            new Question(
                "9. Когда использовать getAcquire/setRelease вместо volatile в HFT?",
                List.of(
                    "a) Когда достаточно парной HB (писатель→читатель) и не нужен total order",
                    "b) Для флага ready: producer setRelease(true), consumer getAcquire() — и видит все предшествующие записи",
                    "c) Когда несколько потоков пишут и читают и нужна согласованность их взглядов",
                    "d) Для счётчиков где нужна только атомарность без ordering"
                ),
                Set.of("a", "b"),
                "acquire/release идеальны для publish-subscribe паттерна: один writer, один (или несколько) readers, нет нужды в total order. Несколько независимых writers/readers требуют volatile (seq_cst) или CAS. d) — это opaque/plain."
            ),
            new Question(
                "10. Что такое happens-before (HB) через volatile?",
                List.of(
                    "a) volatile write HB volatile read того же поля (если read видит значение write)",
                    "b) Всё написанное до volatile write видно после volatile read",
                    "c) HB транзитивна: если A HB B и B HB C, то A HB C",
                    "d) volatile гарантирует атомарность составных операций типа i++"
                ),
                Set.of("a", "b", "c"),
                "HB через volatile: write → read создаёт HB-ребро. Транзитивность позволяет строить цепочки. d) неверно: volatile long атомарен для чтения/записи, но i++ = read+increment+write — три операции, не атомарны вместе."
            ),
            new Question(
                "11. Какой ordering использовать для каждого случая?",
                List.of(
                    "a) Счётчик только в одном треде: plain (no ordering)",
                    "b) Флаг stop: один пишет, один читает → setRelease/getAcquire",
                    "c) Общий счётчик N потоков: compareAndSet (seq_cst CAS)",
                    "d) Проверка «был ли объект инициализирован»: getOpaque достаточно"
                ),
                Set.of("a", "b", "c"),
                "a) Локальный счётчик — plain. b) Publisher/consumer flag — release/acquire. c) Конкурентный инкремент — CAS (seq_cst). d) неверно: инициализация объекта требует acquire чтобы видеть все поля — нужен getAcquire."
            ),
            new Question(
                "12. compareAndSet в VarHandle — какой ordering?",
                List.of(
                    "a) seq_cst — и на success, и на failure",
                    "b) weakCompareAndSet использует relaxed ordering",
                    "c) compareAndExchange позволяет задать кастомный ordering",
                    "d) CAS всегда acquire/release, никогда не seq_cst"
                ),
                Set.of("a", "b", "c"),
                "compareAndSet = seq_cst. weakCompareAndSetPlain / weakCompareAndSetAcquire / weakCompareAndSetRelease — ослабленные варианты с меньшими гарантиями. compareAndExchange позволяет указать onSuccess/onFailure ordering. d) неверно."
            )
        );
    }
}
