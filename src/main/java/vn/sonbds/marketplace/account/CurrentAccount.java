package vn.sonbds.marketplace.account;

import org.springframework.security.core.Authentication;

public final class CurrentAccount {
    private CurrentAccount() {
    }

    public static Account from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Account account)) {
            return null;
        }
        return account;
    }

    public static boolean isAdmin(Account account) {
        return account != null && account.getRoles().contains(Role.ADMIN);
    }
}
