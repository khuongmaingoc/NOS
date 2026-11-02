package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Inquiry> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
