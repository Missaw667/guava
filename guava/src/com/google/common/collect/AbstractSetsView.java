package com.google.common.collect;

import java.util.AbstractSet;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

/** Super-classe pour supprimer la duplication des méthodes de modification interdises */
abstract class AbstractSetsView<E extends @Nullable Object> extends AbstractSet<E> {

    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(@Nullable Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeAll(Collection<?> oldElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean retainAll(Collection<?> elementsToKeep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }
}