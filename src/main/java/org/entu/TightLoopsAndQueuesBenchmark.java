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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 3, time = 3)
public class TightLoopsAndQueuesBenchmark {

    private final int capacity = 1_000_000;

    private final ManyToOneConcurrentArrayQueue<Integer> agronaArrayQueue =
            new ManyToOneConcurrentArrayQueue<>(capacity);

    private int value = 1;

    private final RateLimiter rateLimiter = RateLimiter.create(1_000_000);

    private int testRate = 12500;
    private final RateLimiter testRateLimiter = RateLimiter.create(testRate);

    //    @Benchmark
    public void testRateLimiter() {
        rateLimiter.acquire();
    }

    @Setup
    public void setup() {
        agronaArrayQueue.clear();
    }

    @Benchmark
    @Group("g")
    @GroupThreads(1)
    public void testPlainAdd(Blackhole eatUp) {
        eatUp.consume(agronaArrayQueue.offer(++value));
        // Simulation of load
        eatUp.consume(testRateLimiter.acquire());
    }

    @Benchmark
    @Group("g")
    @GroupThreads(1)
    public Integer testPlainPoll() {
        Integer poll = null;
        while (poll == null) {
            poll = agronaArrayQueue.poll();
        }
        return poll;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(TightLoopsAndQueuesBenchmark.class.getSimpleName())
                        .forks(1)
                        .build();

        new Runner(opt).run();
    }

}
