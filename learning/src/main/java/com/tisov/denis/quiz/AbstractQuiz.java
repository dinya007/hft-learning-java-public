package com.tisov.denis.quiz;

import java.util.List;
import java.util.Scanner;

public abstract class AbstractQuiz implements Quiz {

    private final Scanner in = new Scanner(System.in);
    private int score = 0;
    private int total = 0;

    @Override
    public final void run() {
        System.out.println("Формат: введи буквы ответов через пробел (например: a c)\n");
        runQuestions();
        System.out.println("\n=== Итог: " + score + " / " + total + " ===");
        printVerdict(score, total);
    }

    protected final void runQuestions() {
        for (Question q : getQuestions()) {
            ask(q);
        }
    }

    protected abstract List<Question> getQuestions();

    protected void ask(Question q) {
        if (q.getPreamble() != null) System.out.println(q.getPreamble());
        total++;
        System.out.println("\n" + q.getText());
        q.getOptions().forEach(System.out::println);
        System.out.print("> ");
        String line = in.nextLine().trim().toLowerCase();
        var given = java.util.Set.of(line.split("\\s+"));
        boolean ok = given.equals(q.getCorrect());
        if (ok) {
            score++;
            System.out.println("✓ верно");
        } else {
            System.out.println("✗ неверно. Правильный ответ: " + String.join(" ", q.getCorrect()));
        }
        System.out.println("→ " + q.getExplanation());
    }

    protected void printVerdict(int score, int total) {
        double pct = total == 0 ? 0 : (100.0 * score / total);
        if (pct >= 90)      System.out.println("Отлично — все чекпоинты темы закрыты.");
        else if (pct >= 70) System.out.println("Хорошо, но перечитай разделы где ошибся.");
        else if (pct >= 50) System.out.println("Шатко — нужен ещё раз повтор темы.");
        else                System.out.println("Нужен повтор всей темы с нуля.");
    }
}
