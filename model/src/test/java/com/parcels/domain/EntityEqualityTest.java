package com.parcels.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityEqualityTest {

    @Test
    void unsavedEntitiesAreNotEqual() {
        Category a = new Category();
        Category b = new Category();

        assertNotEquals(a, b);
        assertEquals(a, a);
    }

    @Test
    void entitiesWithSameIdAreEqual() {
        Category a = Category.builder().id(1L).name("Food").build();
        Category b = Category.builder().id(1L).name("Renamed").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entitiesWithDifferentIdsAreNotEqual() {
        assertNotEquals(Category.builder().id(1L).build(), Category.builder().id(2L).build());
    }

    @Test
    void differentEntityTypesWithSameIdAreNotEqual() {
        assertNotEquals(Category.builder().id(1L).build(), Transaction.builder().id(1L).build());
    }

    @Test
    void entityStaysFindableInSetAfterIdIsAssigned() {
        Transaction transaction = new Transaction();
        Set<Transaction> set = new HashSet<>();
        set.add(transaction);

        transaction.setId(42L); // what persist does

        assertTrue(set.contains(transaction));
    }

    @Test
    void twoUnsavedEntitiesAreBothKeptInSet() {
        Set<Category> set = new HashSet<>();
        set.add(new Category());
        set.add(new Category());

        assertEquals(2, set.size());
    }

    @Test
    void builderInitialisesCategoriesToEmptyList() {
        Transaction transaction = Transaction.builder().description("no categories").build();

        assertNotNull(transaction.getCategories());
        assertTrue(transaction.getCategories().isEmpty());
    }
}
