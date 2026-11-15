package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.StudentGuardianLink;

import java.util.List;
import java.util.Optional;

public class GuardianController {

    private final GuardianDao guardianDao;
    private final StudentGuardianDao studentGuardianDao;

    public GuardianController(GuardianDao guardianDao, StudentGuardianDao studentGuardianDao) {
        this.guardianDao = guardianDao;
        this.studentGuardianDao = studentGuardianDao;
    }

    public List<Guardian> listGuardians() {
        return guardianDao.findAll();
    }

    public Optional<Guardian> getGuardian(int id) {
        return guardianDao.findById(id);
    }

    public List<Guardian> searchGuardians(String term) {
        return guardianDao.searchByNameOrDocument(term);
    }

    public int createGuardian(Guardian guardian) {
        return guardianDao.insert(guardian);
    }

    public boolean updateGuardian(Guardian guardian) {
        return guardianDao.update(guardian);
    }

    public boolean deleteGuardian(int id) {
        return guardianDao.deleteById(id);
    }

    public List<StudentGuardianLink> listLinksByStudent(int studentId) {
        return studentGuardianDao.findByStudentId(studentId);
    }

    public List<StudentGuardianLink> listLinksByGuardian(int guardianId) {
        return studentGuardianDao.findByGuardianId(guardianId);
    }

    public boolean linkStudentGuardian(StudentGuardianLink link) {
        return studentGuardianDao.link(link);
    }

    public boolean unlinkStudentGuardian(int studentId, int guardianId) {
        return studentGuardianDao.unlink(studentId, guardianId);
    }
}
