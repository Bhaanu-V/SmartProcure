package com.smartprocure.repository;

import com.smartprocure.entity.ApprovalHistory;
import com.smartprocure.entity.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByPurchaseRequestIdOrderByCreatedAtDesc(Long purchaseRequestId);

    List<ApprovalHistory> findByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);

    List<ApprovalHistory> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<ApprovalHistory> findByPurchaseRequestIdAndStageOrderByCreatedAtDesc(Long purchaseRequestId, ApprovalStage stage);
}
