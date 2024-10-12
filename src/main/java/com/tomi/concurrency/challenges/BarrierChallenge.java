package com.tomi.concurrency.challenges;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Barrier {
    private final int numberOfThreads;
    private int size;
    private final Lock lock;
    private final Condition waitingLine;
    private final Condition inProgress;
    private boolean barrierReleased;
    private boolean acceptingIncomingThreads;

    public Barrier(final int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.size = 0;
        this.lock = new ReentrantLock();
        this.waitingLine = lock.newCondition();
        this.inProgress = lock.newCondition();
        this.barrierReleased = false;
        this.acceptingIncomingThreads = true;
    }

    public void blockUntilConditionIsMet() throws InterruptedException {
        lock.lock();
            
        while (!acceptingIncomingThreads) 
            waitingLine.await();
 
        size++;
         
        System.out.println("Thread checking in ...");

        if (size == numberOfThreads) {
            acceptingIncomingThreads = false;
            barrierReleased = true;
            inProgress.signalAll();
        } else {
            while (!barrierReleased)
                inProgress.await();
        }

        size--;

        if (size == 0) {
            acceptingIncomingThreads = true;
            barrierReleased = false;
            waitingLine.signalAll();
        }
 
        System.out.println("Thread checking out ...");
        
        lock.unlock();
    }
}

public class BarrierChallenge {
    
    private final Barrier barrier = new Barrier(4);

    public void doStuff() {
        try {
            Thread.sleep(1000);
            barrier.blockUntilConditionIsMet();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException {

        final var thread1 = new Thread(this::doStuff);
        final var thread2 = new Thread(this::doStuff);
        final var thread3 = new Thread(this::doStuff);
        final var thread4 = new Thread(this::doStuff);
        
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
    }
}
