package com.google.common.collect;


import java.util.AbstractSet;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.math.IntMath;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

public final class SetsMath {
    private SetsMath() {}

    // Dans SetsMath.java
    public static <B> Set<List<B>> cartesianProduct(List<? extends Set<? extends B>> sets) {
        return CartesianSet.create(sets);
    }

    private static final class SubSet<E> extends AbstractSet<E> {
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

    private static final class PowerSet<E> extends AbstractSet<Set<E>> {
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
                    return new SubSet<>(inputSet, setBits);
                }
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

    /**
     * Returns the set of all subsets of {@code set} of size {@code size}. For example, {@code
     * combinations(ImmutableSet.of(1, 2, 3), 2)} returns the set {@code {{1, 2}, {1, 3}, {2, 3}}}.
     *
     * <p>Elements appear in these subsets in the same iteration order as they appeared in the input
     * set. The order in which these subsets appear in the outer set is undefined.
     *
     * <p>The returned set and its constituent sets use {@code equals} to decide whether two elements
     * are identical, even if the input set uses a different concept of equivalence.
     *
     * <p><i>Performance notes:</i> the memory usage of the returned set is only {@code O(n)}. When
     * the result set is constructed, the input set is merely copied. Only as the result set is
     * iterated are the individual subsets created. Each of these subsets occupies an additional O(n)
     * memory but only for as long as the user retains a reference to it. That is, the set returned by
     * {@code combinations} does not retain the individual subsets.
     *
     * @param set the set of elements to take combinations of
     * @param size the number of elements per combination
     * @return the set of all combinations of {@code size} elements from {@code set}
     * @throws IllegalArgumentException if {@code size} is not between 0 and {@code set.size()}
     *     inclusive
     * @throws NullPointerException if {@code set} is or contains {@code null}
     * @since 23.0
     */
    public static <E> Set<Set<E>> combinations(Set<E> set, int size) {
        ImmutableMap<E, Integer> index = Maps.indexMap(set);
        checkNonnegative(size, "size");
        checkArgument(size <= index.size(), "size (%s) must be <= set.size() (%s)", size, index.size());
        if (size == 0) {
            return ImmutableSet.of(ImmutableSet.of());
        } else if (size == index.size()) {
            return ImmutableSet.of(index.keySet());
        }
        return new AbstractSet<Set<E>>() {
            @Override
            public boolean contains(@Nullable Object o) {
                if (o instanceof Set) {
                    Set<?> s = (Set<?>) o;
                    return s.size() == size && index.keySet().containsAll(s);
                }
                return false;
            }

            @Override
            public Iterator<Set<E>> iterator() {
                return new AbstractIterator<Set<E>>() {
                    final BitSet bits = new BitSet(index.size());

                    @Override
                    protected @Nullable Set<E> computeNext() {
                        if (bits.isEmpty()) {
                            bits.set(0, size);
                        } else {
                            int firstSetBit = bits.nextSetBit(0);
                            int bitToFlip = bits.nextClearBit(firstSetBit);

                            if (bitToFlip == index.size()) {
                                return endOfData();
                            }

                            /*
                             * The current set in sorted order looks like
                             * {firstSetBit, firstSetBit + 1, ..., bitToFlip - 1, ...}
                             * where it does *not* contain bitToFlip.
                             *
                             * The next combination is
                             *
                             * {0, 1, ..., bitToFlip - firstSetBit - 2, bitToFlip, ...}
                             *
                             * This is lexicographically next if you look at the combinations in descending order
                             * e.g. {2, 1, 0}, {3, 1, 0}, {3, 2, 0}, {3, 2, 1}, {4, 1, 0}...
                             */

                            bits.set(0, bitToFlip - firstSetBit - 1);
                            bits.clear(bitToFlip - firstSetBit - 1, bitToFlip);
                            bits.set(bitToFlip);
                        }
                        BitSet copy = (BitSet) bits.clone();
                        return new AbstractSet<E>() {
                            @Override
                            public boolean contains(@Nullable Object o) {
                                Integer i = index.get(o);
                                return i != null && copy.get(i);
                            }

                            @Override
                            public Iterator<E> iterator() {
                                return new AbstractIterator<E>() {
                                    int i = -1;

                                    @Override
                                    protected @Nullable E computeNext() {
                                        i = copy.nextSetBit(i + 1);
                                        if (i == -1) {
                                            return endOfData();
                                        }
                                        return index.keySet().asList().get(i);
                                    }
                                };
                            }

                            @Override
                            public int size() {
                                return size;
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return IntMath.binomial(index.size(), size);
            }

            @Override
            public String toString() {
                return "Sets.combinations(" + index.keySet() + ", " + size + ")";
            }
        };
    }

    /**
     * Returns the set of all possible subsets of {@code set}. For example, {@code
     * powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{}, {1}, {2}, {1, 2}}}.
     *
     * <p>Elements appear in these subsets in the same iteration order as they appeared in the input
     * set. The order in which these subsets appear in the outer set is undefined. Note that the power
     * set of the empty set is not the empty set, but a one-element set containing the empty set.
     *
     * <p>The returned set and its constituent sets use {@code equals} to decide whether two elements
     * are identical, even if the input set uses a different concept of equivalence.
     *
     * <p><i>Performance notes:</i> while the power set of a set with size {@code n} is of size {@code
     * 2^n}, its memory usage is only {@code O(n)}. When the power set is constructed, the input set
     * is merely copied. Only as the power set is iterated are the individual subsets created, and
     * these subsets themselves occupy only a small constant amount of memory.
     *
     * @param set the set of elements to construct a power set from
     * @return the power set, as an immutable set of immutable sets
     * @throws IllegalArgumentException if {@code set} has more than 30 unique elements (causing the
     *     power set size to exceed the {@code int} range)
     * @throws NullPointerException if {@code set} is or contains {@code null}
     * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at Wikipedia</a>
     * @since 4.0
     */
    public static <E> Set<Set<E>> powerSet(Set<E> set) {
        return new PowerSet<E>(set);
    }

    private static final class CartesianSet<E> extends ForwardingCollection<List<E>>
            implements Set<List<E>> {
        private final transient ImmutableList<ImmutableSet<E>> axes;
        private final transient CartesianList<E> delegate;

        static <E> Set<List<E>> create(List<? extends Set<? extends E>> sets) {
            ImmutableList.Builder<ImmutableSet<E>> axesBuilder = new ImmutableList.Builder<>(sets.size());
            for (Set<? extends E> set : sets) {
                ImmutableSet<E> copy = ImmutableSet.copyOf(set);
                if (copy.isEmpty()) {
                    return ImmutableSet.of();
                }
                axesBuilder.add(copy);
            }
            ImmutableList<ImmutableSet<E>> axes = axesBuilder.build();
            ImmutableList<List<E>> listAxes =
                    new ImmutableList<List<E>>() {
                        @Override
                        public int size() {
                            return axes.size();
                        }

                        @Override
                        public List<E> get(int index) {
                            return axes.get(index).asList();
                        }

                        @Override
                        boolean isPartialView() {
                            return true;
                        }

                        // redeclare to help optimizers with b/310253115
                        @SuppressWarnings("RedundantOverride")
                        @Override
                        @J2ktIncompatible
                        @GwtIncompatible
                        Object writeReplace() {
                            return super.writeReplace();
                        }
                    };
            return new CartesianSet<E>(axes, new CartesianList<E>(listAxes));
        }

        private CartesianSet(ImmutableList<ImmutableSet<E>> axes, CartesianList<E> delegate) {
            this.axes = axes;
            this.delegate = delegate;
        }

        @Override
        protected Collection<List<E>> delegate() {
            return delegate;
        }

        @Override
        public boolean contains(@Nullable Object object) {
            if (!(object instanceof List)) {
                return false;
            }
            List<?> list = (List<?>) object;
            if (list.size() != axes.size()) {
                return false;
            }
            int i = 0;
            for (Object o : list) {
                if (!axes.get(i).contains(o)) {
                    return false;
                }
                i++;
            }
            return true;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this
            if (object instanceof CartesianSet) {
                CartesianSet<?> that = (CartesianSet<?>) object;
                return this.axes.equals(that.axes);
            }
            if (object instanceof Set) {
                Set<?> that = (Set<?>) object;
                return this.size() == that.size() && this.containsAll(that);
            }
            return false;
        }

        @Override
        public int hashCode() {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this

            // It's a weird formula, but tests prove it works.
            int adjust = size() - 1;
            for (int i = 0; i < axes.size(); i++) {
                adjust *= 31;
                adjust = ~~adjust;
                // in GWT, we have to deal with integer overflow carefully
            }
            int hash = 1;
            for (Set<E> axis : axes) {
                hash = 31 * hash + (size() / axis.size() * axis.hashCode());

                hash = ~~hash;
            }
            hash += adjust;
            return ~~hash;
        }
    }

}
