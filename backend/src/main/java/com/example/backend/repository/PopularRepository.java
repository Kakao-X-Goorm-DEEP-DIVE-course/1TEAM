package com.example.backend.repository;

import com.example.backend.entity.Popular;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularRepository extends JpaRepository<Popular, Integer> {
}