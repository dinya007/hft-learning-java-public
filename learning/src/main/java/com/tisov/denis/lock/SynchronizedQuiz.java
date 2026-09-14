package com.tisov.denis.lock;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class SynchronizedQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "synchronized";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Из чего складывается стоимость synchronized? (несколько)",
                List.of(
                    "a) Состояние монитора: biased / thin / inflated",
                    "b) JIT-оптимизации: lock elision, lock coarsening",
                    "c) Contention: uncontended vs contended",
                    "d) Версия JDK всегда одинаково влияет"
                ),
                Set.of("a", "b", "c"),
                "cost = state × JIT × contention × hardware + side-effects (safepoints) + Loom. Версия JDK влияет (biased removed в JDK 15), но ответ d некорректен как абсолют."
            ),
            new Question(
                "2. Какова примерная стоимость uncontended thin lock (JDK 15+) на x86?",
                List.of(
                    "a) ~1 ns (как biased был до JDK 15)",
                    "b) 3–7 ns (CAS на mark word)",
                    "c) ~15 ns (как inflated uncontended)",
                    "d) µs+ (как contended inflated)"
                ),
                Set.of("b"),
                "Thin lock (lightweight) uncontended = lock cmpxchg на mark word = 3–7 ns. Biased был ~1 ns, но удалён в JDK 15. Inflated uncontended ~15 ns. Contended µs+ (park/unpark)."
            ),
            new Question(
                "3. Что произошло с biased locking в JDK 15?",
                List.of(
                    "a) Disabled по умолчанию (JEP 374), удалён позже",
                    "b) Полностью удалён именно в JDK 15",
                    "c) Перенесён в ZGC",
                    "d) Включён по умолчанию для legacy кода"
                ),
                Set.of("a"),
                "JEP 374: biased locking disabled по умолчанию в JDK 15, флаг -XX:+UseBiasedLocking ещё работал. Полностью удалён в JDK 18+. Следствие: thin lock = новый дешёвый uncontended fast path."
            ),
            new Question(
                "4. Что триггерит inflation thin → inflated? (несколько)",
                List.of(
                    "a) Contention — CAS-fail: другой тред уже держит lock",
                    "b) Object.wait() / notify() / notifyAll()",
                    "c) System.identityHashCode(obj) на locked object",
                    "d) JFR / serviceability monitor enter event"
                ),
                Set.of("a", "b", "c", "d"),
                "Все четыре. wait/notify требует ObjectMonitor::WaitSet. identityHashCode конкурирует за биты mark word. JFR требует identity монитора для событий. Contention — самый частый триггер."
            ),
            new Question(
                "5. Lock elision через escape analysis — когда C2 убирает synchronized?",
                List.of(
                    "a) Когда объект не escape'нул из метода (non-escape)",
                    "b) Когда метод помечен final",
                    "c) Когда объект создан в том же методе и не передан наружу",
                    "d) Всегда для StringBuffer"
                ),
                Set.of("a", "c"),
                "C2 убирает monitorenter/exit если объект non-escape: не возвращается, не записывается в поле, не передаётся в метод который JIT не смог заинлайнить. StringBuffer.append — типичный пример, но только если sb локальный."
            ),
            new Question(
                "6. Что делает lock coarsening?",
                List.of(
                    "a) Сшивает соседние synchronized-блоки на одном мониторе в один",
                    "b) Заменяет synchronized на volatile",
                    "c) Убирает synchronized через escape analysis",
                    "d) Укрупняет блок синхронизации чтобы снизить число enter/exit"
                ),
                Set.of("a", "d"),
                "sb.append(a); sb.append(b); sb.append(c) → три enter/exit превращаются в один. Контролируется -XX:+EliminateLocks (on by default). Отличие от elision: объект может escape'нуть, просто соседние локи сшиваются."
            ),
            new Question(
                "7. Что такое permit в LockSupport.park/unpark?",
                List.of(
                    "a) Бинарный токен (0/1) на каждый тред — нельзя накапливать",
                    "b) park() потребляет permit или блокируется; unpark() выдаёт permit",
                    "c) Повторный unpark() накапливает permits — следующий park() вернётся дважды",
                    "d) pre-signal через unpark() сохраняется и park() вернётся сразу"
                ),
                Set.of("a", "b", "d"),
                "Permit бинарный: max 1. unpark() перед park() = pre-signal, park() вернётся сразу. Повторный unpark() no-op (не накапливает). Spurious wakeup допустим — всегда в цикле условия."
            ),
            new Question(
                "8. Чем LockSupport.park/unpark отличается от Object.wait/notify?",
                List.of(
                    "a) park не требует владения монитором, wait — требует",
                    "b) unpark адресует конкретный тред, notify будит случайный из WaitSet",
                    "c) pre-signal через unpark сохраняется; pre-notify теряется",
                    "d) park и wait одинаковы — просто разные API"
                ),
                Set.of("a", "b", "c"),
                "Три ключевых отличия: (1) нет требования к владению монитором, (2) адресный unpark vs неадресный notify, (3) permit sticky при pre-signal vs потеря pre-notify."
            ),
            new Question(
                "9. Почему synchronized на hot path запрещён в HFT? (несколько)",
                List.of(
                    "a) Inflation/deflation/revocation требуют safepoint → TTSP jitter",
                    "b) Contended inflated lock = park/unpark = µs+ латентность",
                    "c) Cache-line в Modified у соседнего core → сотни ns на coherence",
                    "d) synchronized всегда дороже volatile"
                ),
                Set.of("a", "b", "c"),
                "Три реальных причины. d) неверно: uncontended thin lock 3–7 ns, volatile read бесплатен на x86 — в некоторых сценариях thin lock сравним с volatile. Главное — непредсказуемость safepoints и contention."
            ),
            new Question(
                "10. Как виртуальные треды (Loom) взаимодействуют с synchronized до JDK 24?",
                List.of(
                    "a) vthread пиннится на carrier-тред при входе в synchronized блок",
                    "b) Pinning = carrier недоступен для других vthreads → теряется масштабируемость",
                    "c) JEP 491 (JDK 24) позволяет vthread unmount под synchronized",
                    "d) synchronized полностью поддерживает vthreads с JDK 21"
                ),
                Set.of("a", "b", "c"),
                "До JDK 24: synchronized пиннит vthread на carrier — mount/unmount невозможен внутри блока. JEP 491 в JDK 24 фиксит это. JNI ещё пиннит. d) неверно — JDK 21 не решил proблему pinning."
            ),
            new Question(
                "11. На x86 thin lock enter = lock cmpxchg. Что это означает для exit-fence?",
                List.of(
                    "a) lock cmpxchg даёт StoreLoad-fence — exit не требует дополнительного барьера",
                    "b) Нужен отдельный mfence на выходе",
                    "c) lock cmpxchg это только LoadLoad барьер",
                    "d) x86 вообще не нужны барьеры для синхронизации"
                ),
                Set.of("a"),
                "На x86 lock-префикс даёт full memory barrier (StoreLoad). Поэтому enter уже содержит нужный барьер и exit-fence бесплатен. На ARM/POWER нужны отдельные барьеры на входе и выходе."
            ),
            new Question(
                "12. Почему vthread не подходит для HFT hot path? (несколько)",
                List.of(
                    "a) mount/unmount overhead в µs — слишком дорого для hot path",
                    "b) Аллокация continuation frame на heap = GC pressure",
                    "c) JIT хуже инлайнит через continuation boundary",
                    "d) vthread нельзя использовать с AtomicLong"
                ),
                Set.of("a", "b", "c"),
                "Три реальных причины. vthreads уместны в admin/control plane (REST, мониторинг, recovery) — там latency не критична. d) неверно — AtomicLong работает с vthread без ограничений."
            )
        );
    }
}
