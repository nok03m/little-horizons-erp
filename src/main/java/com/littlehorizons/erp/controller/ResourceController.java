package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.ResourceLoanTransactionDao;
import com.littlehorizons.erp.dao.SchoolResourceDao;
import com.littlehorizons.erp.dao.StudentLoanDao;
import com.littlehorizons.erp.dao.TeacherLoanDao;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.SchoolResource;
import com.littlehorizons.erp.model.StudentLoan;
import com.littlehorizons.erp.model.TeacherLoan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceController {

    public static final String STATUS_AVAILABLE = "Disponible";
    public static final String STATUS_LOANED = "Prestado";
    public static final String LOAN_ACTIVE = "Activo";
    public static final String LOAN_RETURNED = "Devuelto";
    public static final String LOAN_OVERDUE = "Vencido";

    private final ResourceCategoryDao resourceCategoryDao;
    private final SchoolResourceDao schoolResourceDao;
    private final StudentLoanDao studentLoanDao;
    private final TeacherLoanDao teacherLoanDao;
    private final ResourceLoanTransactionDao loanTransactionDao;

    public ResourceController(ResourceCategoryDao resourceCategoryDao,
                              SchoolResourceDao schoolResourceDao,
                              StudentLoanDao studentLoanDao,
                              TeacherLoanDao teacherLoanDao) {
        this.resourceCategoryDao = resourceCategoryDao;
        this.schoolResourceDao = schoolResourceDao;
        this.studentLoanDao = studentLoanDao;
        this.teacherLoanDao = teacherLoanDao;
        this.loanTransactionDao = new ResourceLoanTransactionDao();
    }

    public List<ResourceCategory> listCategories() {
        return resourceCategoryDao.findAll();
    }

    public Optional<ResourceCategory> getCategory(int id) {
        return resourceCategoryDao.findById(id);
    }

    public int createCategory(ResourceCategory category) {
        return resourceCategoryDao.insert(category);
    }

    public List<SchoolResource> listResources() {
        return schoolResourceDao.findAll();
    }

    public List<SchoolResource> listAvailableResources() {
        return schoolResourceDao.findByStatus(STATUS_AVAILABLE);
    }

    public Optional<SchoolResource> getResource(int id) {
        return schoolResourceDao.findById(id);
    }

    public int createResource(SchoolResource resource) {
        return schoolResourceDao.insert(resource);
    }

    public boolean updateResource(SchoolResource resource) {
        return schoolResourceDao.update(resource);
    }

    public boolean deleteResource(int id) {
        return schoolResourceDao.deleteById(id);
    }

    public List<StudentLoan> listStudentLoans() {
        return studentLoanDao.findAll();
    }

    public List<TeacherLoan> listTeacherLoans() {
        return teacherLoanDao.findAll();
    }

    public int loanToStudent(StudentLoan loan) {
        requireAvailable(loan.getResourceId());
        loan.setStatus(LOAN_ACTIVE);
        return loanTransactionDao.loanToStudent(loan);
    }

    public int loanToTeacher(TeacherLoan loan) {
        requireAvailable(loan.getResourceId());
        loan.setStatus(LOAN_ACTIVE);
        return loanTransactionDao.loanToTeacher(loan);
    }

    public boolean returnStudentLoan(int loanId, LocalDate returnDate, String observations) {
        StudentLoan loan = studentLoanDao.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Student loan not found."));
        if (LOAN_RETURNED.equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is already returned.");
        }
        boolean returned = loanTransactionDao.returnStudentLoan(loanId, returnDate, observations);
        if (!returned) {
            throw new IllegalStateException("Loan was already returned or changed by another operation.");
        }
        return true;
    }

    public boolean returnTeacherLoan(int loanId, LocalDate returnDate, String observations) {
        TeacherLoan loan = teacherLoanDao.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher loan not found."));
        if (LOAN_RETURNED.equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is already returned.");
        }
        boolean returned = loanTransactionDao.returnTeacherLoan(loanId, returnDate, observations);
        if (!returned) {
            throw new IllegalStateException("Loan was already returned or changed by another operation.");
        }
        return true;
    }

    public int refreshOverdueLoans() {
        return studentLoanDao.markOverdue() + teacherLoanDao.markOverdue();
    }

    public List<String> listOverdueLoansReport() {
        refreshOverdueLoans();
        List<String> lines = new ArrayList<>();
        studentLoanDao.findOverdue().forEach(loan -> lines.add("[STUDENT] " + loan));
        teacherLoanDao.findOverdue().forEach(loan -> lines.add("[TEACHER] " + loan));
        return lines;
    }

    private SchoolResource requireAvailable(int resourceId) {
        SchoolResource resource = schoolResourceDao.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
        if (!STATUS_AVAILABLE.equals(resource.getStatus())) {
            throw new IllegalStateException("Resource is not available. Current status: " + resource.getStatus());
        }
        return resource;
    }
}
