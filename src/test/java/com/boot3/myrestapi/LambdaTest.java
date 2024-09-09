package com.boot3.myrestapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

/*
    함수형 인터페이스는 추상 메서드를 한 개만 가지고 있는 인터페이스이다.
    추상 메서드를 오버라이딩 하는 구문을 람다식으로 작성할 수 있다.
 */
public class LambdaTest {
    @Test @Disabled
    public void runnable() {
        // class MyRunnable implements Runnable
        // new Thread(new MyRunnable());

        //1. Anonymous Inner class
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous Inner class");
            }
        });
        t1.start();

        //2. Lambda Expression
        Thread t2 = new Thread(() -> System.out.println("Lambda Expression"));
        t2.start();
    }
    /*
        함수형 인터페이스 ( java.util.function )
        Predicate boolean test(T t) 조건식
        Consumer  void accept(T t) 입력만
        Supplier  T get() 출력만
        Function  R apply(T t) 입출력
        UnaryOperator  Function<T,T>
        BinaryOperator Function<T,T,T>
     */

    @Test //@Disabled
    public void consumer() {
        List<String> list = List.of("aa", "bb", "cc");//Immutable List

        //1. Anonymous Inner class
        list.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println("s = " + s);
            }
        });

        //2.Lambda Expression
        //Consumer 추상 메서드 void accept(T t)
        list.forEach(val -> System.out.println("값 = " + val));

        //3.Method Reference
        list.forEach(System.out::println);
    }

}