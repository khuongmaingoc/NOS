package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByAccountIdAndListingId(Long accountId, Long listingId);
    Optional<Favorite> findByAccountIdAndListingId(Long accountId, Long listingId);
    List<Favorite> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
