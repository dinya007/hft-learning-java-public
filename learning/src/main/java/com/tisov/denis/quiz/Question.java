package com.tisov.denis.quiz;

import java.util.List;
import java.util.Set;

public class Question {

    private final String preamble;
    private final String text;
    private final List<String> options;
    private final Set<String> correct;
    private final String explanation;

    public Question(String text, List<String> options, Set<String> correct, String explanation) {
        this(null, text, options, correct, explanation);
    }

    public Question(String preamble, String text, List<String> options, Set<String> correct, String explanation) {
        this.preamble = preamble;
        this.text = text;
        this.options = options;
        this.correct = correct;
        this.explanation = explanation;
    }

    public String getPreamble()      { return preamble; }
    public String getText()          { return text; }
    public List<String> getOptions() { return options; }
    public Set<String> getCorrect()  { return correct; }
    public String getExplanation()   { return explanation; }
}
