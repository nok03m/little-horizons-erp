package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Optional;

public class StudentController {

    private final StudentDao studentDao;

    public StudentController(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    public List<Student> listStudents() {
        return studentDao.findAll();
    }

    public Optional<Student> getStudent(int id) {
        return studentDao.findById(id);
    }

    public List<Student> searchStudents(String term) {
        return studentDao.searchByName(term);
    }

    public int createStudent(Student student) {
        return studentDao.insert(student);
    }

    public boolean updateStudent(Student student) {
        return studentDao.update(student);
    }

    public boolean retireStudent(int id) {
        return studentDao.retireById(id);
    }
}
