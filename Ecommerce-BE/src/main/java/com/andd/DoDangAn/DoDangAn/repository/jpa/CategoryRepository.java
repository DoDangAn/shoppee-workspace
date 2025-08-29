package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByCategoryName(String categoryName);
    
    @Query("SELECT c FROM Category c")
    List<Category> findAllCategory();


    @Query("SELECT c FROM Category c JOIN c.products p WHERE p.id = :productId")
    List<Category> findCategoryByProductId(@Param("productId") String productId);

    // search by name fragment
    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Category> searchByNameFragment(@Param("q") String q);


}
//package com.andd.DoDangAn.DoDangAn.repository;

//import com.andd.DoDangAn.DoDangAn.models.Product;
//import org.springframework.data.jpa.repository.JpaRepository;

//import java.util.List;

//public interface ProductRepository extends JpaRepository<Product,String> {
//  List<Product> findByProductName(String productName);
//}
