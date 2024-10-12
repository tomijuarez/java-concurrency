package com.tomi.concurrency.challenges;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Problem Statement
 * A blocking queue is defined as a queue which blocks the caller of the enqueue method if there’s no more capacity 
 * to add the new item being enqueued. 
 * Similarly, the queue blocks the dequeue caller if there are no items in the queue. 
 * Also, the queue notifies a blocked enqueuing thread when space becomes available and a blocked dequeuing thread when an item becomes available in the queue.
 */

interface ConcurrentQueue {
    void enqueue(final int item) throws InterruptedException;
    int dequeue() throws InterruptedException;
}

class BlockingQueueWithMonitors implements ConcurrentQueue {
    private int capacity;
    private int size;
    private int back;
    private int front;
    private Integer[] items;

    public BlockingQueueWithMonitors(final int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.back = -1;
        this.front = 0;
        this.items = new Integer[capacity];
    }

    public void enqueue(final int item) throws InterruptedException {
        synchronized(this) {
            while (size == capacity) {
                this.wait();
            }
            back = (back + 1) % capacity;
            items[back] = item;
            size++;
            this.notifyAll();
        }
    }

    public int dequeue() throws InterruptedException {
        synchronized(this) {
            while (size == 0) {
                this.wait();
            }
            final var peak = items[front];
            size--;
            front = (front + 1) % capacity;
            this.notifyAll();
            return peak;
        }
    }
}

class BlockingQueueWithSemaphores implements ConcurrentQueue {
    private int capacity;
    private int back;
    private int front;
    private Integer[] items;
    private Semaphore writeSemaphore;
    private Semaphore readSemaphore;
    private final Object monitor;

    public BlockingQueueWithSemaphores(final int capacity) {
        this.capacity = capacity;
        this.back = -1;
        this.front = 0;
        this.items = new Integer[capacity];
        this.writeSemaphore = new Semaphore(capacity);
        // we could use CountingSemaphore(capacity, 0) to ensure we can have at most "capacity" threads at a time, but 0 initially.
        this.readSemaphore = new Semaphore(0);
        // we might use two monitors for performance to seggregate reads from writes.
        this.monitor = new Object();
    }

    public void enqueue(final int item) throws InterruptedException {
        writeSemaphore.acquire();

        synchronized (monitor) {
            back = (back + 1) % capacity;
            items[back] = item;
        }

        readSemaphore.release();
    }

    public int dequeue() throws InterruptedException {
        readSemaphore.acquire();
        
        int peak;
        synchronized (monitor) {
            peak = items[front];
            front = (front + 1) % capacity;
        }

        writeSemaphore.release();
        
        return peak;
    }
}

class BlockingQueueWithLocks implements ConcurrentQueue {
    private int capacity;
    private int size;
    private int back;
    private int front;
    private Integer[] items;
    private Lock lock;

    public BlockingQueueWithLocks(final int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.back = -1;
        this.front = 0;
        this.items = new Integer[capacity];
        this.lock = new ReentrantLock();
    }

    public void enqueue(final int item) throws InterruptedException {
        try {
            lock.lock();
            while (size == capacity) {
                // we already checked and size is equal to capacity.
                // release the lock so another thread can wake up and check.
                // if you don't do that, you may have a deadlock.
                lock.unlock();
                //before checking again, we lock.
                lock.lock();
            }
            back = (back + 1) % capacity;
            items[back] = item;
            size++;
        } finally {
            lock.unlock();
        }
    }

    public int dequeue() throws InterruptedException {
        try {
            lock.lock();
            while (size == 0) {
                lock.unlock();

                lock.lock();
            }
            final var peak = items[front];
            size--;
            front = (front + 1) % capacity;
            return peak;
        } finally {
            lock.unlock();
        }
    }
}

class BlockingQueueWithLocksAndConditionVariable implements ConcurrentQueue {
    private int capacity;
    private int size;
    private int back;
    private int front;
    private Integer[] items;
    private Lock lock;
    private Condition isEmpty;
    private Condition isFull;

    public BlockingQueueWithLocksAndConditionVariable(final int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.back = -1;
        this.front = 0;
        this.items = new Integer[capacity];
        this.lock = new ReentrantLock();
        this.isFull = lock.newCondition();
        this.isEmpty = lock.newCondition();
    }

    public void enqueue(final int item) throws InterruptedException {
        try {
            lock.lock();
            while (size == capacity) {
                isFull.await();
            }
            back = (back + 1) % capacity;
            items[back] = item;
            size++;
            isEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int dequeue() throws InterruptedException {
        try {
            lock.lock();
            while (size == 0) {
                isEmpty.await();
            }
            final var peak = items[front];
            size--;
            front = (front + 1) % capacity;
            isFull.signalAll();
            return peak;
        } finally {
            lock.unlock();
        }
    }
}

public class BlockingQueueChallenge { 
    class Publisher implements Runnable {
        private final ConcurrentQueue queue;

        public Publisher(final ConcurrentQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            final var prefix = (int) Thread.currentThread().threadId();
            try {
                for (var i = 0; i < 30; i++) {
                    queue.enqueue(prefix + i);
                }
            } catch (final InterruptedException e) {

            }
        }
    }

    class Consumer implements Runnable {
        private final ConcurrentQueue queue;

        public Consumer(final ConcurrentQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                for (var i = 0; i < 60; i++) {
                    var pop = queue.dequeue();
                    System.out.println("Reading element " + pop);
                }
            } catch (InterruptedException e) {

            }
        }    
    }

    public void execute() {
        final var queue = new BlockingQueueWithSemaphores(10);

        final var publisher1 = new Publisher(queue);
        final var publisher2 = new Publisher(queue);
        final var consumer1 = new Consumer(queue);

        final var t1 = new Thread(publisher1);
        final var t2 = new Thread(publisher2);
        final var t3 = new Thread(consumer1);
    
        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {

        } finally {

        }
    }
}

