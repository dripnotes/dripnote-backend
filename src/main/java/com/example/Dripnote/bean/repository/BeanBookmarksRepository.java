package com.example.Dripnote.bean.repository;

import com.example.Dripnote.bean.entity.BeanBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeanBookmarksRepository extends JpaRepository<BeanBookmarkEntity, Long> {
}
