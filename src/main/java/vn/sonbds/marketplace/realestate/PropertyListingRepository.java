package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long> {
    List<PropertyListing> findAllByOrderByCreatedAtDesc();
    List<PropertyListing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
