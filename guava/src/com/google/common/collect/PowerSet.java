package com.google.common.collect;

import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;


import static com.google.common.base.Preconditions.checkArgument;

class PowerSet<E> extends AbstractSet<Set<E>>{
    final ImmutableMap<E, Integer> inputSet;

    PowerSet(Set<E> input) {
        checkArgument(
                input.size() <= 30, "Too many elements to create power set: %s > 30", input.size());
        this.inputSet = Maps.indexMap(input);
    }

    @Override
    public int size() {
        return 1 << inputSet.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator<Set<E>> iterator() {
        return new AbstractIndexedListIterator<Set<E>>(size()) {
            @Override
            protected Set<E> get(int setBits) {
                return new SubSet<>(inputSet, setBits);            }
        };
    }

    @Override
    public boolean contains(@Nullable Object obj) {
        if (obj instanceof Set) {
            Set<?> set = (Set<?>) obj;
            return inputSet.keySet().containsAll(set);
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PowerSet) {
            PowerSet<?> that = (PowerSet<?>) obj;
            return inputSet.keySet().equals(that.inputSet.keySet());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        /*
         * The sum of the sums of the hash codes in each subset is just the sum of
         * each input element's hash code times the number of sets that element
         * appears in. Each element appears in exactly half of the 2^n sets, so:
         */
        return inputSet.keySet().hashCode() << (inputSet.size() - 1);
    }

    @Override
    public String toString() {
        return "powerSet(" + inputSet + ")";
    }
}
