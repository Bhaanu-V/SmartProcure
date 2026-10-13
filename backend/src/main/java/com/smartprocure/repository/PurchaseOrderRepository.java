package com.smartprocure.repository;

import com.smartprocure.entity.POStatus;
import com.smartprocure.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    @Query("SELECT po FROM PurchaseOrder po " +
           "LEFT JOIN FETCH po.lineItems " +
           "LEFT JOIN FETCH po.supplier " +
           "LEFT JOIN FETCH po.department " +
           "LEFT JOIN FETCH po.requester " +
           "LEFT JOIN FETCH po.purchaseRequest " +
           "WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    Optional<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    Page<PurchaseOrder> findByStatus(POStatus status, Pageable pageable);

    Page<PurchaseOrder> findByRequesterId(Long requesterId, Pageable pageable);

    long countByStatus(POStatus status);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(:supplierId IS NULL OR po.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR po.status = :status) AND " +
           "(:query IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<PurchaseOrder> filterOrders(
            @Param("supplierId") Long supplierId,
            @Param("status") POStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}
