package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandProjectRepository extends JpaRepository<LandProject, Long> {
    List<LandProject> findAllByOrderByCreatedAtDesc();
    List<LandProject> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
