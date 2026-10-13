package com.smartprocure.repository;

import com.smartprocure.entity.DepartmentBudget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentBudgetRepository extends JpaRepository<DepartmentBudget, Long> {

    Optional<DepartmentBudget> findByDepartmentIdAndFiscalYear(Long departmentId, int fiscalYear);

    List<DepartmentBudget> findByDepartmentId(Long departmentId);

    List<DepartmentBudget> findByFiscalYear(int fiscalYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DepartmentBudget b WHERE b.department.id = :departmentId AND b.fiscalYear = :fiscalYear")
    Optional<DepartmentBudget> findByDepartmentIdAndFiscalYearForUpdate(
            @Param("departmentId") Long departmentId,
            @Param("fiscalYear") int fiscalYear
    );
}
