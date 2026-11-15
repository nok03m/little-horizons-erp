package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;

import java.util.List;
import java.util.Optional;

public class TeacherController {

    private final TeacherDao teacherDao;
    private final TeacherAssignmentDao teacherAssignmentDao;

    public TeacherController(TeacherDao teacherDao, TeacherAssignmentDao teacherAssignmentDao) {
        this.teacherDao = teacherDao;
        this.teacherAssignmentDao = teacherAssignmentDao;
    }

    public List<Teacher> listTeachers() {
        return teacherDao.findAll();
    }

    public Optional<Teacher> getTeacher(int id) {
        return teacherDao.findById(id);
    }

    public List<Teacher> searchTeachers(String term) {
        return teacherDao.search(term);
    }

    public int createTeacher(Teacher teacher) {
        return teacherDao.insert(teacher);
    }

    public boolean updateTeacher(Teacher teacher) {
        return teacherDao.update(teacher);
    }

    public boolean deleteTeacher(int id) {
        return teacherDao.deleteById(id);
    }

    public List<TeacherAssignment> listAssignments() {
        return teacherAssignmentDao.findAll();
    }

    public List<TeacherAssignment> listAssignmentsByTeacher(int teacherId) {
        return teacherAssignmentDao.findByTeacherId(teacherId);
    }

    public int createAssignment(TeacherAssignment assignment) {
        return teacherAssignmentDao.insert(assignment);
    }

    public boolean deleteAssignment(int id) {
        return teacherAssignmentDao.deleteById(id);
    }
}
