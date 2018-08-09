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

import org.apache.lucene.util.ArrayUtil;

import static org.apache.lucene.util.RamUsageEstimator.NUM_BYTES_OBJECT_REF;

final class CharBlockPool {

    public char[][] buffers = new char[10][];
    int numBuffer;

    int bufferUpto = -1;                        // Which buffer we are upto
    public int charUpto = DocumentsWriter.CHAR_BLOCK_SIZE;             // Where we are in head buffer

    public char[] buffer;                              // Current head buffer
    public int charOffset = -DocumentsWriter.CHAR_BLOCK_SIZE;          // Current head offset
    final private DocumentsWriter docWriter;

    public CharBlockPool(DocumentsWriter docWriter) {
        this.docWriter = docWriter;
    }

    public void reset() {
        docWriter.recycleCharBlocks(buffers, 1 + bufferUpto);
        bufferUpto = -1;
        charUpto = DocumentsWriter.CHAR_BLOCK_SIZE;
        charOffset = -DocumentsWriter.CHAR_BLOCK_SIZE;
    }

    public void nextBuffer() {
        if (1 + bufferUpto == buffers.length) {
            char[][] newBuffers = new char[ArrayUtil.oversize(buffers.length + 1,
                    NUM_BYTES_OBJECT_REF)][];
            System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
            buffers = newBuffers;
        }
        buffer = buffers[1 + bufferUpto] = docWriter.getCharBlock();
        bufferUpto++;

        charUpto = 0;
        charOffset += DocumentsWriter.CHAR_BLOCK_SIZE;
    }
}

