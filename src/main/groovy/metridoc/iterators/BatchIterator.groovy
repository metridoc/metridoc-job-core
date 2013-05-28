/*
 * Copyright 2010 Trustees of the University of Pennsylvania Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package metridoc.iterators

import com.google.common.collect.AbstractIterator

class BatchIterator extends AbstractIterator {
    /**
     * the wrapped iterator
     */
    Iterator iterator
    int batchSize

    BatchIterator(Iterator iterator, int batchSize) {
        assert iterator: "iterator cannot be null"
        assert batchSize > 0: "batch size must be greater than 0"
        this.iterator = iterator
        this.batchSize = batchSize
    }

    @Override
    protected Object computeNext() {
        def result = []
        for (int i = 0; i < batchSize; i++) {
            if (iterator.hasNext()) {
                result.add(iterator.next())
            } else {
                break
            }
        }

        if (!result) {
            return endOfData()
        }

        return result
    }
}
