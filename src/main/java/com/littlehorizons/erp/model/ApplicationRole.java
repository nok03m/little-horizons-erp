package com.littlehorizons.erp.model;

import java.util.Set;

/** Roles that control access to top-level application modules. */
public enum ApplicationRole {
    ADMIN,
    ACADEMIC,
    FINANCE,
    LIBRARIAN;

    public Set<Integer> allowedMenuOptions() {
        return switch (this) {
            case ADMIN -> Set.of(1, 2, 3, 4, 5, 6, 7);
            case ACADEMIC -> Set.of(1, 2, 3, 4);
            case FINANCE -> Set.of(5);
            case LIBRARIAN -> Set.of(6);
        };
    }

    public boolean canAccessMenuOption(int option) {
        return allowedMenuOptions().contains(option);
    }
}
