package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;

import java.util.List;
import java.util.Optional;

public class EnrollmentController {

    private final EnrollmentDao enrollmentDao;
    private final GradeDao gradeDao;

    public EnrollmentController(EnrollmentDao enrollmentDao, GradeDao gradeDao) {
        this.enrollmentDao = enrollmentDao;
        this.gradeDao = gradeDao;
    }

    public List<Enrollment> listEnrollments() {
        return enrollmentDao.findAll();
    }

    public List<Enrollment> listByStudent(int studentId) {
        return enrollmentDao.findByStudentId(studentId);
    }

    public Optional<Enrollment> getEnrollment(int id) {
        return enrollmentDao.findById(id);
    }

    public List<Grade> listGrades() {
        return gradeDao.findAll();
    }

    public Optional<Grade> getGrade(int id) {
        return gradeDao.findById(id);
    }

    public int createEnrollment(Enrollment enrollment) {
        return enrollmentDao.insert(enrollment);
    }

    public boolean updateEnrollment(Enrollment enrollment) {
        return enrollmentDao.update(enrollment);
    }

    public boolean deleteEnrollment(int id) {
        return enrollmentDao.deleteById(id);
    }
}
