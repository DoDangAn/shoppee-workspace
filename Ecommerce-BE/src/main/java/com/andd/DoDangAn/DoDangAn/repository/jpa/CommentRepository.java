package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Comment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends CrudRepository<Comment, String> {
    List<Comment> findByProductId(String productID);
    //List<Comment> findByProductIdAndUserId(Product productID, String userID);
    void deleteByProductId(String productID);
}