package com.se201411.project.mooncake.product.repository;

import com.se201411.project.mooncake.entity.ProductVideo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVideoRepository extends JpaRepository<ProductVideo, Long> {

    List<ProductVideo> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    @EntityGraph(attributePaths = "product")
    List<ProductVideo> findAllByOrderByIdDesc();
}
