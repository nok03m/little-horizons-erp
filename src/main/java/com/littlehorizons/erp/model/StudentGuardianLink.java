package com.littlehorizons.erp.model;

/**
 * Domain model mapped to table estudiante_acudiente.
 */
public class StudentGuardianLink {

    private int studentId;
    private int guardianId;
    private boolean primaryContact;
    private boolean emergencyContact;
    private String studentName;
    private String guardianName;
    private String relationship;

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(int guardianId) {
        this.guardianId = guardianId;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        this.primaryContact = primaryContact;
    }

    public boolean isEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(boolean emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public String toString() {
        return String.format("student #%d %s <-> guardian #%d %s (%s) primary=%s emergency=%s",
                studentId,
                studentName == null ? "" : studentName,
                guardianId,
                guardianName == null ? "" : guardianName,
                relationship == null ? "-" : relationship,
                primaryContact,
                emergencyContact);
    }
}
