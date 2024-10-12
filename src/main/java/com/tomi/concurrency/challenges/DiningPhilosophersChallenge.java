package com.tomi.concurrency.challenges;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Fork {
    private boolean busy;

    public Fork() {
        this.busy = false;
    }

    public boolean isFree() {
        return !this.busy;
    }

    public void markAsBusy() {
        this.busy = true;
    }

    public void markAsFree() {
        this.busy = false;
    }
}

class Philosopher implements Runnable {
    private final Fork[] forks;
    private final int positionInTable;
    private final int leftForkPosition;
    private final int rightForkPosition;
    private final Lock lock;

    public Philosopher(final int positionInTable, final Fork[] forks, final int forksNumber, final Lock lock) {
        this.forks = forks;
        this.positionInTable = positionInTable;
        this.lock = lock;
        this.leftForkPosition = (positionInTable + forksNumber - 1) % forksNumber;
        this.rightForkPosition = positionInTable;
    }

    private boolean areBothForksFree() {
        return forks[leftForkPosition].isFree() && forks[rightForkPosition].isFree();
    }

    private void useBothForks() {
        forks[leftForkPosition].markAsBusy();
        forks[rightForkPosition].markAsBusy();
    }

    private void releaseBothForks() {
        forks[leftForkPosition].markAsFree();
        forks[rightForkPosition].markAsFree();
    }

    private void eat() {
        lock.lock();
        
        while (!areBothForksFree()) {
            lock.unlock();

            lock.lock();
        }

        useBothForks();

        System.out.println("Philosopher " + positionInTable + " started eating.");
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println("Philosopher " + positionInTable + " finished eating.");            

        releaseBothForks();

        lock.unlock();
    }

    @Override
    public void run() {
        eat();
    }
}

// Plantear mejora con lock en fork para no bloquear el acceso al arreglo entero.
public class DiningPhilosophersChallenge {
    private final Fork[] forks;

    public DiningPhilosophersChallenge() {
        this.forks = new Fork[] {
            new Fork(), new Fork(), new Fork(), new Fork(), new Fork()
        };
    }

    public void serveDinner() throws InterruptedException {
        final var lock = new ReentrantLock(); 
        final var threads = new Thread[5]; 
        
        for (int i = 0; i < 5; i++)
            threads[i] = new Thread(new Philosopher(i, forks, 5, lock));
        
        for (int i = 0; i < 5; i++) {
            threads[i].start();
        }

        for (int i = 0; i < 5; i++) {
            threads[i].join();
        }
    }
}
