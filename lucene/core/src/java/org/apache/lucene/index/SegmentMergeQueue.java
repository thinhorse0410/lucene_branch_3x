package org.apache.lucene.index;

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

import org.apache.lucene.util.PriorityQueue;

import java.io.IOException;

final class SegmentMergeQueue extends PriorityQueue<SegmentMergeInfo> {
    SegmentMergeQueue(int size) {
        initialize(size);
    }

    @Override
    protected final boolean lessThan(SegmentMergeInfo stiA, SegmentMergeInfo stiB) {
        int comparison = stiA.term.compareTo(stiB.term);
        if (comparison == 0)
            return stiA.base < stiB.base;
        else
            return comparison < 0;
    }

    final void close() throws IOException {
        while (top() != null)
            pop().close();
    }

}
