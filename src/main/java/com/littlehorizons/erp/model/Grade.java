package com.littlehorizons.erp.model;

/**
 * Domain model mapped to table grados.
 */
public class Grade {

    private int id;
    private String name;
    private int numericLevel;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumericLevel() {
        return numericLevel;
    }

    public void setNumericLevel(int numericLevel) {
        this.numericLevel = numericLevel;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s (level %d)", id, name, numericLevel);
    }
}
