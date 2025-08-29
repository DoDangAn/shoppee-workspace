package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Advertise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AdvertiseRepository extends JpaRepository<Advertise, String> {
    @Query("SELECT a FROM Advertise a WHERE a.date=:date")
    List<Advertise> findByDate(Date date);
    @Query("SELECT a FROM Advertise a WHERE a.role=:role")
    List<Advertise> findByRole(String role);
}
