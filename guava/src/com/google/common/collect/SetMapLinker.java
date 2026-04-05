package com.google.common.collect;

import com.google.common.base.Predicate;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static java.lang.Math.ceil;

/**
 * Classe utilitaire interne destinée à briser le cycle de dépendance
 * bidirectionnel entre Sets et Maps.
 */
final class SetMapLinker {
    private SetMapLinker() {}
    private static final double LOAD_FACTOR = 0.75;

    // Extrait de Sets.java (Ligne 405)
    static <E> Set<E> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<E, Boolean>());
    }

    // Extrait de Maps.java (Ligne 3568)
    static <E> ImmutableMap<E, Integer> indexMap(Collection<E> list) {
        ImmutableMap.Builder<E, Integer> builder = new ImmutableMap.Builder<>(list.size());
        int i = 0;
        for (E e : list) {
            builder.put(e, i++);
        }
        return builder.buildOrThrow();
    }

    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < com.google.common.primitives.Ints.MAX_POWER_OF_TWO) {
            // This seems to be consistent across JDKs. The capacity argument to HashMap and LinkedHashMap
            // ends up being used to compute a "threshold" size, beyond which the internal table
            // will be resized. That threshold is ceilingPowerOfTwo(capacity*loadFactor), where
            // loadFactor is 0.75 by default. So with the calculation here we ensure that the
            // threshold is equal to ceilingPowerOfTwo(expectedSize). There is a separate code
            // path when the first operation on the new map is putAll(otherMap). There, prior to
            // https://github.com/openjdk/jdk/commit/3e393047e12147a81e2899784b943923fc34da8e, a bug
            // meant that sometimes a too-large threshold is calculated. However, this new threshold is
            // independent of the initial capacity, except that it won't be lower than the threshold
            // computed from that capacity. Because the internal table is only allocated on the first
            // write, we won't see copying because of the new threshold. So it is always OK to use the
            // calculation here.
            return (int) ceil(expectedSize / LOAD_FACTOR);
        }
        return Integer.MAX_VALUE; // any large value
    }

    /** An implementation for {@link Set#equals(Object)}. */
    static boolean equalsImpl(Set<?> s, @Nullable Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set<?> o = (Set<?>) object;

            try {
                return s.size() == o.size() && s.containsAll(o);
            } catch (NullPointerException | ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    /** An implementation for {@link Set#hashCode()}. */
    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            hashCode += o != null ? o.hashCode() : 0;

            hashCode = ~~hashCode;
            // Needed to deal with unusual integer overflow in GWT.
        }
        return hashCode;
    }

    /**
     * Returns the elements of {@code unfiltered} that satisfy a predicate. The returned set is a live
     * view of {@code unfiltered}; changes to one affect the other.
     *
     * <p>The resulting set's iterator does not support {@code remove()}, but all other set methods
     * are supported. When given an element that doesn't satisfy the predicate, the set's {@code
     * add()} and {@code addAll()} methods throw an {@link IllegalArgumentException}. When methods
     * such as {@code removeAll()} and {@code clear()} are called on the filtered set, only elements
     * that satisfy the filter will be removed from the underlying set.
     *
     * <p>The returned set isn't threadsafe or serializable, even if {@code unfiltered} is.
     *
     * <p>Many of the filtered set's methods, such as {@code size()}, iterate across every element in
     * the underlying set and determine which elements satisfy the filter. When a live view is
     * <i>not</i> needed, it may be faster to copy {@code Iterables.filter(unfiltered, predicate)} and
     * use the copy.
     *
     * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>, as documented at
     * {@link Predicate#apply}. Do not provide a predicate such as {@code
     * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals. (See {@link
     * Iterables#filter(Iterable, Class)} for related functionality.)
     *
     * <p><b>Java 8+ users:</b> many use cases for this method are better addressed by {@link
     * java.util.stream.Stream#filter}. This method is not being deprecated, but we gently encourage
     * you to migrate to streams.
     */
    // TODO(kevinb): how to omit that last sentence when building GWT javadoc?
    public static <E extends @Nullable Object> Set<E> filter(
            Set<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof SortedSet) {
            return filter((SortedSet<E>) unfiltered, predicate);
        }
        if (unfiltered instanceof Sets.FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            Sets.FilteredSet<E> filtered = (Sets.FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate = com.google.common.base.Predicates.and(filtered.predicate, predicate);
            return new Sets.FilteredSet<>((Set<E>) filtered.unfiltered, combinedPredicate);
        }

        return new Sets.FilteredSet<>(checkNotNull(unfiltered), checkNotNull(predicate));
    }

    /**
     * Returns an unmodifiable view of the specified navigable set. This method allows modules to
     * provide users with "read-only" access to internal navigable sets. Query operations on the
     * returned set "read through" to the specified set, and attempts to modify the returned set,
     * whether direct or via its collection views, result in an {@code UnsupportedOperationException}.
     *
     * <p>The returned navigable set will be serializable if the specified navigable set is
     * serializable.
     *
     * <p><b>Java 8+ users and later:</b> Prefer {@link Collections#unmodifiableNavigableSet}.
     *
     * @param set the navigable set for which an unmodifiable view is to be returned
     * @return an unmodifiable view of the specified navigable set
     * @since 12.0
     */
    public static <E extends @Nullable Object> NavigableSet<E> unmodifiableNavigableSet(
            NavigableSet<E> set) {
        if (set instanceof ImmutableCollection || set instanceof Sets.UnmodifiableNavigableSet) {
            return set;
        }
        return new Sets.UnmodifiableNavigableSet<>(set);
    }

    /**
     * Décomposition d'une méthode hybride (commande + requête).
     * Sépare la recherche des éléments à supprimer de l'action de suppression.
     */
    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
        List<Object> toRemove = findElementsToRemove(set, iterator);
        return performRemoval(set, toRemove);
    }

    /** Identifie les éléments présents dans les deux structures */
    private static List<Object> findElementsToRemove(Set<?> set, Iterator<?> iterator) {
        List<Object> found = new ArrayList<>();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (set.contains(element)) {
                found.add(element);
            }
        }
        return found;
    }

    /** Modifie l'état de l'objet */
    private static boolean performRemoval(Set<?> set, List<Object> elements) {
        if (elements.isEmpty()) {
            return false;
        }
        return set.removeAll(elements);
    }

    /**
     * {@link AbstractSet} substitute without the potentially-quadratic {@code removeAll}
     * implementation.
     */
    abstract static class ImprovedAbstractSet<E extends @Nullable Object> extends AbstractSet<E> {
        @Override
        public boolean removeAll(Collection<?> c) {
            return removeAllImpl(this, c.iterator());
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return super.retainAll(checkNotNull(c)); // GWT compatibility
        }
    }

    public static <E extends @Nullable Object> HashSet<E> newHashSetWithExpectedSize(
            int expectedSize) {
        return new HashSet<>(SetMapLinker.capacity(expectedSize));
    }

}