package com.google.common.collect;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

class CartesianSet<E> extends ForwardingCollection<List<E>>
        implements Set<List<E>> {
    private final transient ImmutableList<ImmutableSet<E>> axes;
    private final transient CartesianList<E> delegate;


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
        if (object instanceof com.google.common.collect.CartesianSet) {
            com.google.common.collect.CartesianSet<?> that = (com.google.common.collect.CartesianSet<?>) object;
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
                    @com.google.common.annotations.J2ktIncompatible
                    @com.google.common.annotations.GwtIncompatible
                    Object writeReplace() {
                        return super.writeReplace();
                    }
                };
        return new com.google.common.collect.CartesianSet<E>(axes, new CartesianList<E>(listAxes));
    }

    private CartesianSet(ImmutableList<ImmutableSet<E>> axes, CartesianList<E> delegate) {
        this.axes = axes;
        this.delegate = delegate;
    }

    @Override
    protected Collection<List<E>> delegate() {
        return delegate;
    }

}