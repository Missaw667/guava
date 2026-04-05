package com.google.common.collect;

import junit.framework.TestCase;
import java.util.List;
import java.util.Set;
import com.google.common.annotations.GwtIncompatible;

/**
 * Tests unitaires pour la nouvelle classe extraite SetsMath.
 * Valide les fonctionnalités de combinations, powerSet et cartesianProduct.
 */
public class SetsMathTest extends TestCase {

    public void testPowerSetEmpty() {
        Set<Integer> empty = ImmutableSet.of();
        Set<Set<Integer>> powerSet = SetsMath.powerSet(empty);
        assertEquals(1, powerSet.size());
        assertEquals(empty, powerSet.iterator().next());
    }

    public void testPowerSetSize() {
        Set<Integer> set = ImmutableSet.of(1, 2, 3);
        Set<Set<Integer>> powerSet = SetsMath.powerSet(set);
        assertEquals(8, powerSet.size()); // 2^3
    }

    public void testPowerSetTooLarge() {
        Set<Integer> bigSet = ContiguousSet.create(
                Range.closed(1, 31), DiscreteDomain.integers());
        try {
            SetsMath.powerSet(bigSet);
            fail("Aurait dû lever une IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCombinations() {
        Set<Integer> set = ImmutableSet.of(1, 2, 3);
        Set<Set<Integer>> combinations = SetsMath.combinations(set, 2);
        assertEquals(3, combinations.size()); // 3 choose 2 = 3
        for (Set<Integer> combo : combinations) {
            assertEquals(2, combo.size());
        }
    }

    public void testCombinationsSizeZero() {
        Set<Integer> set = ImmutableSet.of(1, 2, 3);
        Set<Set<Integer>> combinations = SetsMath.combinations(set, 0);
        assertEquals(1, combinations.size());
        assertTrue(combinations.iterator().next().isEmpty());
    }

    public void testCartesianProduct_containsEmpty() {
        Set<Integer> set1 = ImmutableSet.of(1, 2);
        Set<Integer> empty = ImmutableSet.of();
        Set<List<Integer>> product = SetsMath.cartesianProduct(ImmutableList.of(set1, empty));
        assertTrue(product.isEmpty());
    }
}