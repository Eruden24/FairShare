package io.github.eruden24.fairshare.repository;

import io.github.eruden24.fairshare.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}
