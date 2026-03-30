package com.google.common.collect;

import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

class SubSet<E> extends AbstractSet<E> {
    private final ImmutableMap<E, Integer> inputSet;
    private final int mask;

    SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
        this.inputSet = inputSet;
        this.mask = mask;
    }

    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator<E>() {
            final ImmutableList<E> elements = inputSet.keySet().asList();
            int remainingSetBits = mask;

            @Override
            public boolean hasNext() {
                return remainingSetBits != 0;
            }

            @Override
            public E next() {
                int index = Integer.numberOfTrailingZeros(remainingSetBits);
                if (index == 32) {
                    throw new NoSuchElementException();
                }
                remainingSetBits &= ~(1 << index);
                return elements.get(index);
            }
        };
    }

    @Override
    public int size() {
        return Integer.bitCount(mask);
    }

    @Override
    public boolean contains(@Nullable Object o) {
        Integer index = inputSet.get(o);
        return index != null && (mask & (1 << index)) != 0;
    }
}