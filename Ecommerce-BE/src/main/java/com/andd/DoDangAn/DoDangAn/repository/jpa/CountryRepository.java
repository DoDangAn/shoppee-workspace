package com.andd.DoDangAn.DoDangAn.repository.jpa;

import com.andd.DoDangAn.DoDangAn.models.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    Optional<Country> findByCountryName(String name);
    Optional<Country> findByCountryId(String countryId);
    long count();
}
