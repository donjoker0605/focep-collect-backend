package org.example.collectfocep.entities.enums;

public enum Priority {
    CRITIQUE("Critique", 1),
    ELEVEE("Élevée", 2),
    NORMALE("Normale", 3),
    FAIBLE("Faible", 4);

    private final String label;
    private final int order;

    Priority(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }

    public boolean isMoreImportantThan(Priority other) {
        return this.order < other.order;
    }
}