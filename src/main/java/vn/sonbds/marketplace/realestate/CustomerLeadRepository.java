package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerLeadRepository extends JpaRepository<CustomerLead, Long> {
    List<CustomerLead> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
