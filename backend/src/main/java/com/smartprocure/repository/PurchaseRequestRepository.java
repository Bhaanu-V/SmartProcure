package com.smartprocure.repository;

import com.smartprocure.entity.PurchaseRequest;
import com.smartprocure.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    Optional<PurchaseRequest> findByRequestNumber(String requestNumber);

    boolean existsByRequestNumber(String requestNumber);

    Page<PurchaseRequest> findByRequesterId(Long requesterId, Pageable pageable);

    Page<PurchaseRequest> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<PurchaseRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<PurchaseRequest> findByStatusIn(Collection<RequestStatus> statuses, Pageable pageable);

    Page<PurchaseRequest> findByDepartmentIdAndStatusIn(Long departmentId, Collection<RequestStatus> statuses, Pageable pageable);

    Page<PurchaseRequest> findByDepartmentIdAndStatus(Long departmentId, RequestStatus status, Pageable pageable);

    long countByStatus(RequestStatus status);

    @Query("SELECT r FROM PurchaseRequest r WHERE " +
           "(:requesterId IS NULL OR r.requester.id = :requesterId) AND " +
           "(:departmentId IS NULL OR r.department.id = :departmentId) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<PurchaseRequest> filterRequests(
            @Param("requesterId") Long requesterId,
            @Param("departmentId") Long departmentId,
            @Param("status") RequestStatus status,
            Pageable pageable
    );
}
