package com.toby.menu.repository;

import com.toby.menu.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    List<Combo> findByActivoTrue();
}
