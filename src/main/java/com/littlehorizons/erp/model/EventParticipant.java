package com.littlehorizons.erp.model;

/**
 * Domain model mapped to table participantes_evento.
 */
public class EventParticipant {

    private int id;
    private int eventId;
    private int studentId;
    private String participationRole;
    private String positionObtained;
    private String observations;
    private String eventName;
    private String studentName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getParticipationRole() {
        return participationRole;
    }

    public void setParticipationRole(String participationRole) {
        this.participationRole = participationRole;
    }

    public String getPositionObtained() {
        return positionObtained;
    }

    public void setPositionObtained(String positionObtained) {
        this.positionObtained = positionObtained;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Override
    public String toString() {
        return String.format("#%d | event:#%d %s | student:#%d %s | role:%s | position:%s",
                id, eventId, eventName == null ? "" : eventName,
                studentId, studentName == null ? "" : studentName,
                participationRole,
                positionObtained == null ? "-" : positionObtained);
    }
}
