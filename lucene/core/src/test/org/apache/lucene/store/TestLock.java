package org.apache.lucene.store;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;

public class TestLock extends LuceneTestCase {

    public void testObtain() {
        LockMock lock = new LockMock();
        Lock.LOCK_POLL_INTERVAL = 10;

        try {
            lock.obtain(Lock.LOCK_POLL_INTERVAL);
            fail("Should have failed to obtain lock");
        } catch (IOException e) {
            assertEquals("should attempt to lock more than once", lock.lockAttempts, 2);
        }
    }

    private class LockMock extends Lock {
        public int lockAttempts;

        @Override
        public boolean obtain() {
            lockAttempts++;
            return false;
        }

        @Override
        public void release() {
            // do nothing
        }

        @Override
        public boolean isLocked() {
            return false;
        }
    }
}
