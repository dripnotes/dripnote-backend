package dripnote.user.repository;

import dripnote.user.domain.User;
import dripnote.user.enums.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Provider별로 ProviderId를 통해 사용자 조회
    Optional<User> findByProviderAndProviderId(UserProvider provider, String providerId);
}
