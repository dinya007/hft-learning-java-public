package com.tisov.denis.jit;

public class ClassDeOptimization {

    private static int counter = 0;

    public static abstract class Animal {
        public abstract void speak();
    }

    public static class Dog extends Animal {
        @Override
        public void speak() {
            ++counter;
        }
    }

    public static class Cat extends Animal {
        @Override
        public void speak() {
            ++counter;
        }
    }

    public static void deOptimize() {
        Animal animal = new Dog();
        for (int i = 0; i < 10_000_000; i++) {
            animal.speak();
        }

        animal = new Cat();
        animal.speak();
        System.out.println(counter);
    }
}
