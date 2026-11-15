package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * Domain model mapped to table asistencias.
 */
public class AttendanceRecord {

    private int id;
    private int studentId;
    private int subjectId;
    private LocalDate attendanceDate;
    private String status;
    private String observations;
    private Integer recordedByTeacherId;
    private String studentName;
    private String subjectName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Integer getRecordedByTeacherId() {
        return recordedByTeacherId;
    }

    public void setRecordedByTeacherId(Integer recordedByTeacherId) {
        this.recordedByTeacherId = recordedByTeacherId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    @Override
    public String toString() {
        return String.format("#%d | student:#%d %s | subject:#%d %s | %s | %s",
                id, studentId, studentName == null ? "" : studentName,
                subjectId, subjectName == null ? "" : subjectName,
                attendanceDate, status);
    }
}
