package com.eventplatform.auth.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {

    @Test
    void testEnumValues() {
        UserRole[] roles = UserRole.values();
        assertEquals(3, roles.length);
    }

    @Test
    void testBuyerRole() {
        UserRole role = UserRole.BUYER;
        assertEquals("BUYER", role.name());
    }

    @Test
    void testOrganiserRole() {
        UserRole role = UserRole.ORGANISER;
        assertEquals("ORGANISER", role.name());
    }

    @Test
    void testAdminRole() {
        UserRole role = UserRole.ADMIN;
        assertEquals("ADMIN", role.name());
    }

    @Test
    void testValueOf() {
        assertEquals(UserRole.BUYER, UserRole.valueOf("BUYER"));
        assertEquals(UserRole.ORGANISER, UserRole.valueOf("ORGANISER"));
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> UserRole.valueOf("INVALID"));
    }

    @Test
    void testEnumOrder() {
        UserRole[] roles = UserRole.values();
        assertEquals(UserRole.BUYER, roles[0]);
        assertEquals(UserRole.ORGANISER, roles[1]);
        assertEquals(UserRole.ADMIN, roles[2]);
    }
}
