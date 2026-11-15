package com.littlehorizons.erp.model;

import java.time.LocalTime;

/**
 * Domain model mapped to table asignaciones_profesor.
 */
public class TeacherAssignment {

    private int id;
    private int teacherId;
    private int subjectId;
    private int academicYear;
    private String section;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String teacherName;
    private String subjectName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    @Override
    public String toString() {
        String schedule = dayOfWeek == null || startTime == null || endTime == null
                ? "schedule:not set"
                : String.format("schedule:%s %s-%s", dayOfWeek, startTime, endTime);
        return String.format("#%d | teacher:#%d %s | subject:#%d %s | year:%d | section:%s | %s",
                id, teacherId, teacherName == null ? "" : teacherName,
                subjectId, subjectName == null ? "" : subjectName,
                academicYear, section == null ? "-" : section, schedule);
    }
}
