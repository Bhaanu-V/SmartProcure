package com.smartprocure.repository;

import com.smartprocure.entity.CatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

    Optional<CatalogItem> findBySku(String sku);

    boolean existsBySku(String sku);

    List<CatalogItem> findBySupplierId(Long supplierId);

    List<CatalogItem> findBySupplierIdAndActiveTrue(Long supplierId);

    List<CatalogItem> findByCategory(String category);

    @Query("SELECT c FROM CatalogItem c WHERE c.active = true AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<CatalogItem> searchCatalog(@Param("query") String query, Pageable pageable);
}
