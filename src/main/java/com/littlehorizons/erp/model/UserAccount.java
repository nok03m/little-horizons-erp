package com.littlehorizons.erp.model;

/** Authenticated application identity. Password hashes are never exposed to views. */
public record UserAccount(String username, ApplicationRole role) {
}
