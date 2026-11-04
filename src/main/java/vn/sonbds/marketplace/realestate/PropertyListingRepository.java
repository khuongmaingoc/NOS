package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PropertyListingRepository extends JpaRepository<PropertyListing, Long>,
        JpaSpecificationExecutor<PropertyListing> {
    List<PropertyListing> findAllByOrderByCreatedAtDesc();
    List<PropertyListing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
