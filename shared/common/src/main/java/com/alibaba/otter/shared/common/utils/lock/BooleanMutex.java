/*
 * Copyright (C) 2010-2101 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.otter.shared.common.utils.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 实现一个互斥实现，基于Cocurrent中的{@linkplain AbstractQueuedSynchronizer}实现了自己的sync <br/>
 * 应用场景：系统初始化/授权控制，没权限时阻塞等待。有权限时所有线程都可以快速通过
 *
 * <pre>
 * false : 代表需要被阻塞挂起，等待mutex变为true被唤醒
 * true : 唤醒被阻塞在false状态下的thread
 *
 * BooleanMutex mutex = new BooleanMutex(true);
 * try {
 *     mutex.get(); //当前状态为true, 不会被阻塞
 * } catch (InterruptedException e) {
 *     // do something
 * }
 *
 * mutex.set(false);
 * try {
 *     mutex.get(); //当前状态为false, 会被阻塞直到另一个线程调用mutex.set(true);
 * } catch (InterruptedException e) {
 *     // do something
 * }
 * </pre>
 *
 * @author jianghang 2011-9-23 上午09:58:03
 * @version 4.0.0
 */
public class BooleanMutex {


    private Sync sync;

    public BooleanMutex() {
        sync = new Sync();
        set(false);
    }

    public BooleanMutex(Boolean mutex) {
        sync = new Sync();
        set(mutex);
    }

    /**
     * 阻塞等待Boolean为true
     *
     * @throws InterruptedException
     */
    public void get() throws InterruptedException {
        sync.innerGet();
    }

    /**
     * 阻塞等待Boolean为true,允许设置超时时间
     *
     * @param timeout
     * @param unit
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        sync.innerGet(unit.toNanos(timeout));
    }

    /**
     * 重新设置对应的Boolean mutex
     *
     * @param mutex
     */
    public void set(Boolean mutex) {
        if (mutex) {
            sync.innerSetTrue();
        } else {
            sync.innerSetFalse();
        }
    }

    public boolean state() {
        return sync.innerState();
    }

    /**
     * Synchronization control for BooleanMutex. Uses AQS sync state to represent run status
     */
    private final class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -7828117401763700385L;

        /**
         * State value representing that TRUE
         */
        private static final int TRUE = 1;
        /**
         * State value representing that FALSE
         */
        private static final int FALSE = 2;

        private boolean isTrue(int state) {
            return (state & TRUE) != 0;
        }

        /**
         * 实现AQS的接口，获取共享锁的判断
         */
        @Override
        protected int tryAcquireShared(int state) {
            // 如果为true，直接允许获取锁对象
            // 如果为false，进入阻塞队列，等待被唤醒
            return isTrue(getState()) ? 1 : -1;
        }

        /**
         * 实现AQS的接口，释放共享锁的判断
         */
        @Override
        protected boolean tryReleaseShared(int ignore) {
            // 始终返回true，代表可以release
            return true;
        }

        boolean innerState() {
            return isTrue(getState());
        }

        void innerGet() throws InterruptedException {
            acquireSharedInterruptibly(0);
        }

        void innerGet(long nanosTimeout) throws InterruptedException, TimeoutException {
            if (!tryAcquireSharedNanos(0, nanosTimeout)) {
                throw new TimeoutException();
            }
        }

        void innerSetTrue() {
            for (; ; ) {
                int s = getState();
                if (s == TRUE) {
                    return; // 直接退出
                }
                // cas更新状态，避免并发更新true操作
                if (compareAndSetState(s, TRUE)) {
                    // 释放一下锁对象，唤醒一下阻塞的Thread
                    releaseShared(0);
                    return;
                }
            }
        }

        void innerSetFalse() {
            for (; ; ) {
                int s = getState();
                if (s == FALSE) {
                    return; // 直接退出
                }
                // cas更新状态，避免并发更新false操作
                if (compareAndSetState(s, FALSE)) {
                    return;
                }
            }
        }

    }
}
