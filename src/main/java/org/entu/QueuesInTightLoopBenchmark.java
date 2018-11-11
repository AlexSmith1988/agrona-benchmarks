/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.entu;

import com.google.common.util.concurrent.RateLimiter;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 1, time = 1)
public class QueuesInTightLoopBenchmark {

    private final int capacity = 10_000_000;

    // Queues under test
    private final Queue<Integer> manyToOneConcurrentArrayQueue = new ManyToOneConcurrentArrayQueue<>(capacity);

    private final Queue<Integer> oneToOneConcurrentArrayQueue = new OneToOneConcurrentArrayQueue<>(capacity);

    private final Queue<Integer> arrayBlockingQueue = new ArrayBlockingQueue<>(capacity);

    private final Queue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();

    private final Queue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<>();

    // payload
    private int value = 1;

    private final RateLimiter messageProcessingImitation = RateLimiter.create(660_000);

    private int testRate = 12500;
    private final RateLimiter systemLoadSimulator = RateLimiter.create(testRate);

    //    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @Group("simple")
    public void testRateLimiter() {
        messageProcessingImitation.acquire();
    }

    @Setup
    public void setup() {
        manyToOneConcurrentArrayQueue.clear();
        oneToOneConcurrentArrayQueue.clear();
        arrayBlockingQueue.clear();
        concurrentLinkedQueue.clear();
        linkedBlockingQueue.clear();
    }

    @Benchmark
    @Group("ManyToOne")
    @GroupThreads(1)
    public void agronaManyToOneOffer(Blackhole eatUp) {
        testPlainOffer(eatUp, manyToOneConcurrentArrayQueue);
    }

    public void testPlainOffer(Blackhole eatUp, Queue<Integer> queue) {
        eatUp.consume(queue.offer(++value));
        // Simulation of load
        eatUp.consume(systemLoadSimulator.acquire());
    }

    @Benchmark
    @Group("ManyToOne")
    @GroupThreads(1)
    public Integer agronaManyToOnePoll() {
        return testPlainPoll(manyToOneConcurrentArrayQueue);
    }

    public Integer testPlainPoll(Queue<Integer> queue) {
        Integer poll = null;
        int n = 0;
        while (poll == null) {
            ++n;
            poll = queue.poll();
        }
        messageProcessingImitation.acquire();
        return poll;
    }

    @Benchmark
    @Group("OneToOne")
    @GroupThreads(1)
    public void agronaOneToOneOffer(Blackhole eatUp) {
        testPlainOffer(eatUp, oneToOneConcurrentArrayQueue);
    }

    @Benchmark
    @Group("OneToOne")
    @GroupThreads(1)
    public Integer agronaOneToOnePoll() {
        return testPlainPoll(oneToOneConcurrentArrayQueue);
    }

    @Benchmark
    @Group("blockingArray")
    @GroupThreads(1)
    public void blockingArrayQueueOffer(Blackhole eatUp) {
        testPlainOffer(eatUp, arrayBlockingQueue);
    }

    @Benchmark
    @Group("blockingArray")
    @GroupThreads(1)
    public Integer blockingArrayQueuePoll() {
        return testPlainPoll(arrayBlockingQueue);
    }

    @Benchmark
    @Group("concurrentLinkedQueue")
    @GroupThreads(1)
    public void concurrentLinkedQueueOffer(Blackhole eatUp) {
        testPlainOffer(eatUp, concurrentLinkedQueue);
    }

    @Benchmark
    @Group("concurrentLinkedQueue")
    @GroupThreads(1)
    public Integer concurrentLinkedQueuePoll() {
        return testPlainPoll(concurrentLinkedQueue);
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    @GroupThreads(1)
    public void linkedBlockingQueueOffer(Blackhole eatUp) {
        testPlainOffer(eatUp, linkedBlockingQueue);
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    @GroupThreads(1)
    public Integer linkedBlockingQueuePoll() {
        return testPlainPoll(linkedBlockingQueue);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(QueuesInTightLoopBenchmark.class.getSimpleName())
                        .forks(1)
                        .build();

        new Runner(opt).run();
    }

}
