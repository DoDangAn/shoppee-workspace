package com.andd.DoDangAn.DoDangAn.repository.jpa;


import com.andd.DoDangAn.DoDangAn.models.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    @Query("SELECT p FROM ProductVariant p WHERE p.product.id = :productId")
    List<ProductVariant> findByProductId(@Param("productId") String productId);
    //@Query("SELECT pv FROM ProductVariant pv WHERE pv.productName.price != pv.productName.newprice")
    //List<ProductVariant> findByProductName();
    //@Query("SELECT p FROM ProductVariant p WHERE p.date=:date AND p.product.categories=:category")
    //List<ProductVariant> findByDateAndCategory(@Param("date") Date date, @Param("catgory") String catgory);
    List<ProductVariant> findByProductName(String productName);
    //@Query("SELECT pv FROM ProductVariant pv WHERE pv.product.productName=: productName AND pv.category.categoryName=:categoryName")
    //List<ProductVariant> findByProductNameAndCategory(@Param("ProductName")String productName, @Param("categoryName") String categoryName);

    void deleteByProductId(String id);

    // FETCH JOIN versions to avoid N+1 when building DTO
    @Query("select distinct pv from ProductVariant pv left join fetch pv.category left join fetch pv.country left join fetch pv.product order by pv.date desc")
    List<ProductVariant> findAllWithJoinsOrderByDateDesc();

    @Query("select distinct pv from ProductVariant pv left join fetch pv.category left join fetch pv.country left join fetch pv.product where pv.productName = :name")
    List<ProductVariant> findByProductNameWithJoins(@Param("name") String name);

    // Unified paginated + filterable + searchable query (sorting supplied via Pageable)
    @Query(value = "select distinct pv from ProductVariant pv " +
        "left join fetch pv.category " +
        "left join fetch pv.country " +
        "left join fetch pv.product " +
        "where (:search is null or lower(pv.productName) like lower(concat('%', :search, '%'))) " +
        "and (:categoryId is null or pv.category.categoryID = :categoryId) " +
        "and (:countryId is null or pv.country.countryId = :countryId)",
        countQuery = "select count(pv) from ProductVariant pv " +
            "where (:search is null or lower(pv.productName) like lower(concat('%', :search, '%'))) " +
            "and (:categoryId is null or pv.category.categoryID = :categoryId) " +
            "and (:countryId is null or pv.country.countryId = :countryId)")
    Page<ProductVariant> searchPaged(@Param("search") String search,
                     @Param("categoryId") String categoryId,
                     @Param("countryId") String countryId,
                     Pageable pageable);
}
