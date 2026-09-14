package com.tisov.denis.quiz;

import com.tisov.denis.disruptor.DisruptorQuiz;
import com.tisov.denis.falsesharing.FalseSharingQuiz;
import com.tisov.denis.gc.GarbageCollectionQuiz;
import com.tisov.denis.jit.JitCompilationQuiz;
import com.tisov.denis.jit.SafePointQuiz;
import com.tisov.denis.lock.SynchronizedQuiz;
import com.tisov.denis.memoryorder.MemoryModelQuiz;

import java.util.List;

public class QuizRunner {

    private static final List<Quiz> QUIZZ_LIST = List.of(
            new GarbageCollectionQuiz()
    );

    public static void run()  {
        for (Quiz q : QUIZZ_LIST) {
            System.out.println("=== " + q.name() + " [" + q.topic() + "] ===");
            q.run();
        }
    }
}
