package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * Domain model mapped to table eventos.
 */
public class SchoolEvent {

    private int id;
    private String name;
    private String description;
    private String eventType;
    private LocalDate eventDate;
    private String location;
    private boolean hasCompetition;
    private Integer organizerTeacherId;
    private String organizerTeacherName;
    private boolean active;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isHasCompetition() {
        return hasCompetition;
    }

    public void setHasCompetition(boolean hasCompetition) {
        this.hasCompetition = hasCompetition;
    }

    public Integer getOrganizerTeacherId() {
        return organizerTeacherId;
    }

    public void setOrganizerTeacherId(Integer organizerTeacherId) {
        this.organizerTeacherId = organizerTeacherId;
    }

    public String getOrganizerTeacherName() {
        return organizerTeacherName;
    }

    public void setOrganizerTeacherName(String organizerTeacherName) {
        this.organizerTeacherName = organizerTeacherName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | %s | %s | competition=%s | organizer:%s | active=%s",
                id, name, eventType, eventDate, hasCompetition,
                organizerTeacherName == null ? "-" : organizerTeacherName, active);
    }
}
