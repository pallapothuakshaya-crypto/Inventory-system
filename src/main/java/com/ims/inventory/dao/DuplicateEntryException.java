package com.ims.inventory.dao;

public class DuplicateEntryException extends DaoException {
    public DuplicateEntryException(String message) {
        super(message);
    }

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}