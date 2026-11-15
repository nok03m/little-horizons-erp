package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.service.AcademicService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AcademicController {

    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AcademicRecordDao academicRecordDao;
    private final AttendanceDao attendanceDao;
    private final GradeDao gradeDao;
    private final AcademicService academicService;

    public AcademicController(SubjectDao subjectDao,
                              SubjectEnrollmentDao subjectEnrollmentDao,
                              AcademicRecordDao academicRecordDao,
                              AttendanceDao attendanceDao,
                              GradeDao gradeDao,
                              AcademicService academicService) {
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.academicRecordDao = academicRecordDao;
        this.attendanceDao = attendanceDao;
        this.gradeDao = gradeDao;
        this.academicService = academicService;
    }

    public List<Subject> listSubjects() {
        return subjectDao.findAll();
    }

    public List<Subject> listSubjectsByGrade(int gradeId) {
        return subjectDao.findByGradeId(gradeId);
    }

    public Optional<Subject> getSubject(int id) {
        return subjectDao.findById(id);
    }

    public int createSubject(Subject subject) {
        return subjectDao.insert(subject);
    }

    public boolean updateSubject(Subject subject) {
        return subjectDao.update(subject);
    }

    public boolean deleteSubject(int id) {
        return subjectDao.deleteById(id);
    }

    public List<Grade> listGrades() {
        return gradeDao.findAll();
    }

    public List<SubjectEnrollment> listSubjectEnrollments() {
        return subjectEnrollmentDao.findAll();
    }

    public List<SubjectEnrollment> listSubjectEnrollmentsByStudent(int studentId) {
        return subjectEnrollmentDao.findByStudentId(studentId);
    }

    public int createSubjectEnrollment(SubjectEnrollment enrollment) {
        return subjectEnrollmentDao.insert(enrollment);
    }

    public boolean updateSubjectEnrollment(SubjectEnrollment enrollment) {
        return subjectEnrollmentDao.update(enrollment);
    }

    public boolean deleteSubjectEnrollment(int id) {
        return subjectEnrollmentDao.deleteById(id);
    }

    public Optional<SubjectEnrollment> getSubjectEnrollment(int id) {
        return subjectEnrollmentDao.findById(id);
    }

    public List<AcademicRecord> listAcademicRecords() {
        return academicRecordDao.findAll();
    }

    public List<AcademicRecord> listAcademicRecordsByStudent(int studentId) {
        return academicRecordDao.findByStudentId(studentId);
    }

    public Optional<AcademicRecord> getAcademicRecord(int id) {
        return academicRecordDao.findById(id);
    }

    public int createAcademicRecord(AcademicRecord record) {
        return academicRecordDao.insert(record);
    }

    public boolean updateAcademicRecord(AcademicRecord record) {
        return academicRecordDao.update(record);
    }

    public boolean deleteAcademicRecord(int id) {
        return academicRecordDao.deleteById(id);
    }

    public BigDecimal studentSubjectAverage(int studentId, int subjectId) {
        return academicService.averageBySubject(academicRecordDao.findByStudentId(studentId), studentId, subjectId);
    }

    public BigDecimal studentGeneralAverage(int studentId, int academicYear, String period) {
        return academicService.studentGeneralAverage(
                academicRecordDao.findByStudentId(studentId), studentId, academicYear, period);
    }

    public BigDecimal courseSubjectAverage(int subjectId, int academicYear, String period) {
        return academicService.courseAverageForSubject(
                academicRecordDao.findBySubjectAndYear(subjectId, academicYear), subjectId, academicYear, period);
    }

    public List<AttendanceRecord> listAttendance() {
        return attendanceDao.findAll();
    }

    public List<AttendanceRecord> listAttendanceByStudent(int studentId) {
        return attendanceDao.findByStudentId(studentId);
    }

    public Optional<AttendanceRecord> getAttendance(int id) {
        return attendanceDao.findById(id);
    }

    public int createAttendance(AttendanceRecord record) {
        return attendanceDao.insert(record);
    }

    public boolean updateAttendance(AttendanceRecord record) {
        return attendanceDao.update(record);
    }

    public boolean deleteAttendance(int id) {
        return attendanceDao.deleteById(id);
    }

    public List<Map.Entry<Integer, Long>> excessiveAbsences(LocalDate from, LocalDate to, long threshold) {
        return academicService.studentsWithExcessiveAbsences(attendanceDao.findAbsencesBetween(from, to), threshold);
    }
}
