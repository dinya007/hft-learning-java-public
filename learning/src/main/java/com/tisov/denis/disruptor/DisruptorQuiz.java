package com.tisov.denis.disruptor;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class DisruptorQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "disruptor";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Почему размер ring buffer в Disruptor должен быть степенью двойки?",
                List.of(
                    "a) Требование Java массивов",
                    "b) index = sequence & (size - 1) — битовая маска вместо дорогого % (modulo)",
                    "c) Для выравнивания по cache line",
                    "d) Иначе CAS не работает корректно"
                ),
                Set.of("b"),
                "Степень двойки позволяет заменить дорогой modulo на битовое AND: slot = seq & (bufferSize - 1). Это одна из ключевых микрооптимизаций латентности Disruptor."
            ),
            new Question(
                "2. Что такое Sequence в Disruptor и зачем вокруг него padding?",
                List.of(
                    "a) Счётчик позиции (long), padding защищает от false sharing на cache line",
                    "b) Массив событий в ring buffer",
                    "c) Ссылка на EventHandler",
                    "d) Padding нужен для выравнивания по странице памяти"
                ),
                Set.of("a"),
                "Sequence: volatile long value, окружённый 7 long padding полями с каждой стороны (LhsPadding + RhsPadding). Итого ~136 байт объект. Padding с обеих сторон гарантирует что value не разделяет cache line ни с предыдущим, ни с последующим объектом. Без padding producer.sequence и consumer.sequence на одной cache line → false sharing → сотни ns."
            ),
            new Question(
                "3. В чём разница SingleProducerSequencer и MultiProducerSequencer?",
                List.of(
                    "a) Single использует простое инкрементирование без CAS, Multi использует CAS для claim",
                    "b) Single быстрее — нет конкуренции за claim слота",
                    "c) Multi поддерживает больший ring buffer",
                    "d) Single не поддерживает batch publish"
                ),
                Set.of("a", "b"),
                "SingleProducer: claim — просто next++, нет CAS. MultiProducer: claim через CAS (compareAndSet). Разница в латентности: single producer на BusySpin — минимально возможная латентность в Disruptor."
            ),
            new Question(
                "4. Какие wait strategies существуют и их trade-offs? (несколько)",
                List.of(
                    "a) BusySpin — минимальная латентность, 100% CPU",
                    "b) Yielding — Thread.yield() между проверками, меньше CPU чем BusySpin",
                    "c) Sleeping — LockSupport.parkNanos(), низкий CPU, высокая латентность",
                    "d) Blocking — lock+condition, наименьший CPU, наибольшая латентность"
                ),
                Set.of("a", "b", "c", "d"),
                "Все четыре корректны. HFT hot path: BusySpin (выделенное ядро). Admin/monitoring: Sleeping или Blocking. Yielding — компромисс между ними."
            ),
            new Question(
                "5. Почему Disruptor быстрее ArrayBlockingQueue? (несколько)",
                List.of(
                    "a) Нет lock/unlock на каждой операции put/take",
                    "b) Pre-allocated события — нет аллокаций на hot path, нет GC pressure",
                    "c) Ring buffer cache-friendly — линейный доступ к памяти",
                    "d) Использует виртуальные треды (Loom)"
                ),
                Set.of("a", "b", "c"),
                "ABQ: lock на каждую операцию + аллокация node + GC. Disruptor: нет локов (CAS или single-writer), pre-allocated events, ring buffer = последовательный доступ к памяти = prefetcher работает эффективно."
            ),
            new Question(
                "6. Что такое gating sequence в Disruptor?",
                List.of(
                    "a) Минимальная sequence среди всех downstream consumers — ограничивает producer",
                    "b) Sequence самого быстрого consumer",
                    "c) Внутренний счётчик ring buffer",
                    "d) Barrier для координации нескольких producers"
                ),
                Set.of("a"),
                "Producer не может обогнать самый медленный consumer больше чем на bufferSize. Gating sequence = min(all consumer sequences). Producer проверяет: nextSeq - gatingSeq < bufferSize, иначе spin/wait."
            ),
            new Question(
                "7. Что происходит когда producer обгоняет consumer на bufferSize слотов?",
                List.of(
                    "a) Событие теряется (drop)",
                    "b) Producer блокируется/спинит согласно WaitStrategy пока consumer не освободит слот",
                    "c) Ring buffer расширяется автоматически",
                    "d) Выбрасывается исключение InsufficientCapacityException"
                ),
                Set.of("b"),
                "Disruptor — back-pressure по дизайну. Producer ждёт пока consumer продвинет свою sequence. Тип ожидания зависит от WaitStrategy. InsufficientCapacityException — только при tryPublishEvent с immediate-fail."
            ),
            new Question(
                "8. Зачем события pre-allocated при старте Disruptor?",
                List.of(
                    "a) Чтобы избежать аллокаций на hot path и снизить GC pressure",
                    "b) Для инициализации ring buffer нулями",
                    "c) JVM требует pre-allocation для массивов > 1MB",
                    "d) Чтобы все объекты оказались на одной странице памяти"
                ),
                Set.of("a"),
                "Producer не создаёт new Event() — он берёт pre-allocated slot, заполняет поля, публикует sequence. Нет аллокаций = нет GC = нет GC safepoints на hot path. Это одно из главных преимуществ для HFT."
            ),
            new Question(
                "9. Как работает двухфазный publish в Disruptor (claim + publish)?",
                List.of(
                    "a) next = sequencer.next() → заполнить event[next] → sequencer.publish(next)",
                    "b) sequencer.publish(event) — одна атомарная операция",
                    "c) event = new Event() → ring.put(event)",
                    "d) claim резервирует слот, publish делает его видимым для consumers"
                ),
                Set.of("a", "d"),
                "Claim (next()) резервирует sequence-номер, но consumer не видит событие. После заполнения полей publish(next) делает событие видимым. Это позволяет batch-claim: long hi = sequencer.next(n); for (seq = lo..hi) fill; publish(lo, hi)."
            ),
            new Question(
                "10. Почему consumer в Disruptor может читать несколько событий за раз?",
                List.of(
                    "a) SequenceBarrier.waitFor(sequence) возвращает максимально доступную sequence",
                    "b) Consumer читает от lastProcessed+1 до availableSeq в цикле — batch consumption",
                    "c) Disruptor автоматически группирует события",
                    "d) Это снижает количество barrier-проверок — amortized overhead"
                ),
                Set.of("a", "b", "d"),
                "waitFor(requested) возвращает availableSeq >= requested. Consumer обрабатывает [lastSeq+1..availableSeq] за один вызов. Batch consumption = меньше барьерных операций на событие = выше throughput."
            ),
            new Question(
                "11. Как Disruptor гарантирует happens-before между producer и consumer?",
                List.of(
                    "a) Через volatile write в cursor (publish) и volatile read в waitFor",
                    "b) Через synchronized блоки",
                    "c) Через explicit memory barrier (fence instruction)",
                    "d) JMM гарантирует порядок автоматически без барьеров"
                ),
                Set.of("a"),
                "publish() делает volatile write курсора → все предшествующие записи в event-поля видны после volatile read в waitFor(). Это стандартная happens-before цепочка через volatile по JMM."
            ),
            new Question(
                "12. Какая комбинация даёт минимальную латентность в Disruptor для HFT?",
                List.of(
                    "a) SingleProducer + BusySpin + выделенное ядро (CPU affinity)",
                    "b) MultiProducer + Blocking + общий thread pool",
                    "c) SingleProducer + Sleeping",
                    "d) MultiProducer + Yielding"
                ),
                Set.of("a"),
                "SingleProducer убирает CAS при claim. BusySpin убирает OS-scheduling latency. CPU affinity убирает context switch и cache migration. Это классический HFT-паттерн: один producer-тред пинится на ядро, consumer — на другое."
            )
        );
    }
}
