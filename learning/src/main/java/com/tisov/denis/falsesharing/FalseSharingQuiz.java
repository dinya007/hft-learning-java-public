package com.tisov.denis.falsesharing;

import com.tisov.denis.quiz.AbstractQuiz;
import com.tisov.denis.quiz.Question;

import java.util.List;
import java.util.Set;

public class FalseSharingQuiz extends AbstractQuiz {

    @Override
    public String topic() {
        return "false-sharing";
    }

    @Override
    protected List<Question> getQuestions() {
        return List.of(
            new Question(
                "1. Что такое false sharing?",
                List.of(
                    "a) Два потока пишут в разные переменные, но на одной cache line — инвалидируют её друг другу",
                    "b) Два потока читают одно поле — конкуренция за данные",
                    "c) Переменные логически не связаны, но физически на одной cache line",
                    "d) Компилятор ошибочно помещает поля в один объект"
                ),
                Set.of("a", "c"),
                "False sharing: физически разные переменные на одной 64-байтной cache line. Core A пишет в x, Core B пишет в y — оба инвалидируют line друг другу через MESI → coherence overhead сотни ns."
            ),
            new Question(
                "2. Какой размер cache line на современных x86 процессорах?",
                List.of(
                    "a) 32 байта",
                    "b) 64 байта",
                    "c) 128 байт",
                    "d) Зависит от процессора, обычно 64 или 128"
                ),
                Set.of("b"),
                "64 байта — стандарт для x86 (Intel/AMD с P4). @Contended ставит 128 байт padding (64 до + 64 после) чтобы учесть adjacent cache line prefetcher который может одновременно fetch-ить соседнюю линию."
            ),
            new Question(
                "3. Что делает @jdk.internal.vm.annotation.Contended?",
                List.of(
                    "a) Добавляет 128 байт padding вокруг поля/класса",
                    "b) Изолирует поле на собственную cache line",
                    "c) Требует -XX:-RestrictContended для работы в пользовательском коде",
                    "d) Это то же самое что ручное добавление long[] padding"
                ),
                Set.of("a", "b", "c"),
                "@Contended: JVM добавляет 128 байт (64+64) — перед и после — чтобы изолировать поле. Требует -XX:-RestrictContended для non-JDK классов. d) схоже по эффекту, но @Contended точнее и устойчивее к JVM layout изменениям."
            ),
            new Question(
                "4. Почему @Contended ставит именно 128 байт, а не 64?",
                List.of(
                    "a) Adjacent cache line prefetcher: CPU может загружать две соседние линии вместе",
                    "b) Для выравнивания по странице памяти (4KB)",
                    "c) Чтобы покрыть объект целиком независимо от его размера",
                    "d) 64 байт недостаточно для long[] полей"
                ),
                Set.of("a"),
                "Некоторые CPU-префетчеры работают парами линий (64+64=128). 128-байтное выравнивание гарантирует что горячее поле не окажется на линии, смежной с чужой переменной."
            ),
            new Question(
                "5. Как JOL (Java Object Layout) помогает диагностировать false sharing?",
                List.of(
                    "a) ClassLayout.parseInstance(obj).toPrintable() показывает реальные offsets полей",
                    "b) Позволяет увидеть попадают ли два поля на одну cache line (64 байта)",
                    "c) Показывает mark word и klass pointer",
                    "d) Автоматически добавляет padding"
                ),
                Set.of("a", "b", "c"),
                "JOL показывает layout объекта: header (mark word + klass pointer), offsets каждого поля, padding добавленный JVM. Зная offsets, можно проверить делятся ли поля одной cache line. d) неверно — JOL только анализирует."
            ),
            new Question(
                "6. Ручной padding vs @Contended — в чём разница?",
                List.of(
                    "a) Ручной padding: long p1,p2,...,p7 перед/после горячего поля — 7×8=56 байт",
                    "b) @Contended устойчивее — JVM не переупорядочит поля как в ручном случае",
                    "c) Ручной padding работает без флагов JVM",
                    "d) @Contended требует флаг, ручной — нет"
                ),
                Set.of("b", "c", "d"),
                "Ручной padding: JVM может переупорядочить поля (особенно после JIT), нет гарантии что p1-p7 окажутся рядом с горячим полем. @Contended — JVM-гарантия layout. Ручной работает без флагов, @Contended требует -XX:-RestrictContended."
            ),
            new Question(
                "7. Как `perf c2c` помогает найти false sharing?",
                List.of(
                    "a) Показывает cache line contention на уровне железа",
                    "b) Детектирует HITM (Hit-Modified) события — load попал в линию в Modified состоянии у другого core",
                    "c) Работает без root привилегий",
                    "d) Указывает конкретные переменные/поля с высоким HITM-счётчиком"
                ),
                Set.of("a", "b", "d"),
                "perf c2c report: HITM = Load попал в Modified линию у соседнего core = классический признак false sharing. Показывает горячие адреса памяти. Обычно требует sudo/perf_event_paranoid. d) с hsdis/символами показывает и Java-поля."
            ),
            new Question(
                "8. Что происходит с MESI при false sharing между двумя cores?",
                List.of(
                    "a) Core A пишет: линия переходит в Modified на Core A",
                    "b) Core B читает ту же линию: Core A должен flush в L3, Core B получает Shared",
                    "c) Core B пишет: отправляет Invalidate, Core A теряет линию",
                    "d) Ping-pong линии через L3: каждая запись = сотни ns latency"
                ),
                Set.of("a", "b", "c", "d"),
                "Полный цикл MESI: Modified → Invalidate → Shared → Modified. Каждый цикл = round-trip через L3 (~40-100 cycles). При высокочастотных записях двух core это деградирует throughput до уровня main memory."
            ),
            new Question(
                "9. Какие поля в Java особенно опасны для false sharing?",
                List.of(
                    "a) Счётчики обновляемые разными потоками (long counter в shared объекте)",
                    "b) Sequence-числа в очередях (head и tail в одном классе)",
                    "c) volatile флаги рядом с другими часто изменяемыми полями",
                    "d) static final константы"
                ),
                Set.of("a", "b", "c"),
                "Классика: head/tail очереди в одном классе — producer пишет tail, consumer пишет head — false sharing. Счётчики разных потоков в массиве (counter[threadId]). volatile флаги рядом с изменяемыми данными. static final — иммутабельны, проблем нет."
            ),
            new Question(
                "10. Как избежать false sharing в массиве счётчиков для N потоков?",
                List.of(
                    "a) Выделить каждому потоку свой объект с @Contended",
                    "b) Использовать long[N * 8] и обращаться через threadId * 8",
                    "c) Использовать AtomicLongArray — он автоматически добавляет padding",
                    "d) ThreadLocal<LongAdder> — каждый тред работает со своей копией"
                ),
                Set.of("a", "b", "d"),
                "c) неверно: AtomicLongArray не добавляет padding между элементами — соседние long в массиве на одной cache line. Правильно: шаг 8 long (64 байт), @Contended на wrapper-объект, или ThreadLocal."
            )
        );
    }
}
