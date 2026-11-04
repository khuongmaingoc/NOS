package vn.sonbds.marketplace.account;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AccountService implements UserDetailsService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account loadUserByUsername(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional
    public Account register(String name, String email, String password, Role role) {
        return register(name, email, password, null, role);
    }

    @Transactional
    public Account register(String name, String email, String password, String phone, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (accounts.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        return accounts.save(new Account(name.trim(), normalizedEmail, passwordEncoder.encode(password), phone, Set.of(role)));
    }
}
