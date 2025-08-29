package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends PagingAndSortingRepository<Product, String> {
    Optional<Product> findById(String id);
    List<Product> findAll();
    void deleteById(String id);

    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> findByNameContaining(@Param("keyword") String keyword, Pageable pageable);


    @Modifying
    @Query("UPDATE Product m SET m.viewCount = m.viewCount + 1 WHERE m.id = :id")
    void incrementViewCount(@Param("id") String id);
    Long count();


    boolean existsById(String id);

    //List<Product> findByCategoryIDAndCountry(String categoryID, String country);


    List<Product> findByCategories_CategoryID(String categoryID);

    @Query("SELECT p FROM Product p WHERE p.releaseDate >= :startDate")
    Page<Product> findByReleaseDateAfter(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.releaseDate <= :endDate")
    Page<Product> findByReleaseDateBefore(@Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.releaseDate BETWEEN :startDate AND :endDate")
    Page<Product> findByReleaseDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);





    Product save(Product product);

    boolean existsByProductName(@NotNull @NotBlank(message = "product name cannot be null") @Size(min = 3, max = 300) String productName);
}
