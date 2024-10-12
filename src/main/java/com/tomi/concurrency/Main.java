package com.tomi.concurrency;

import com.tomi.concurrency.challenges.BarrierChallenge;
import com.tomi.concurrency.challenges.DiningPhilosophersChallenge;

class Main {
    public static void main(String[] args) {
        try {
            //final var blockingQueueChallenge1 = new BlockingQueueChallenge();
            //blockingQueueChallenge1.execute();
            //final var philosophersChallenge = new DiningPhilosophersChallenge();
            //philosophersChallenge.serveDinner();
            final var barrierChallenge = new BarrierChallenge();
            barrierChallenge.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
