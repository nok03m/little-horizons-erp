package com.littlehorizons.erp.model;

/**
 * Domain model mapped to table clubes.
 */
public class Club {

    private int id;
    private String name;
    private String description;
    private Integer supervisorTeacherId;
    private String supervisorTeacherName;
    private int maxCapacity;
    private String meetingSchedule;
    private boolean active;
    private int activeMembers;

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

    public Integer getSupervisorTeacherId() {
        return supervisorTeacherId;
    }

    public void setSupervisorTeacherId(Integer supervisorTeacherId) {
        this.supervisorTeacherId = supervisorTeacherId;
    }

    public String getSupervisorTeacherName() {
        return supervisorTeacherName;
    }

    public void setSupervisorTeacherName(String supervisorTeacherName) {
        this.supervisorTeacherName = supervisorTeacherName;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getMeetingSchedule() {
        return meetingSchedule;
    }

    public void setMeetingSchedule(String meetingSchedule) {
        this.meetingSchedule = meetingSchedule;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(int activeMembers) {
        this.activeMembers = activeMembers;
    }

    @Override
    public String toString() {
        return String.format("#%d | %s | capacity:%d/%d | supervisor:%s | active=%s",
                id, name, activeMembers, maxCapacity,
                supervisorTeacherName == null ? "-" : supervisorTeacherName, active);
    }
}
