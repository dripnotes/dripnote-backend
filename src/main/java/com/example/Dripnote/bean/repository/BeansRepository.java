package com.example.Dripnote.bean.repository;

import com.example.Dripnote.bean.entity.BeanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeansRepository extends JpaRepository<BeanEntity, Long> {
}
