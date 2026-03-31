package com.example.Dripnote.bean.repository;

import com.example.Dripnote.bean.entity.RoasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoastersRepository extends JpaRepository<RoasterEntity, Long> {
}
