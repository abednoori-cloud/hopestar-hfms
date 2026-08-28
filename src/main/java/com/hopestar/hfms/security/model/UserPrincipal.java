package com.hopestar.hfms.security.model;

import com.hopestar.hfms.module.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts the {@link User} JPA entity to Spring Security's
 * {@link UserDetails} contract. Authorities are exposed both as the
 * role itself (prefixed {@code ROLE_}, for {@code hasRole(...)} checks)
 * and as individual permission codes (for the more granular
 * {@code hasAuthority(...)} checks used by privileged-operation gating,
 * approved architecture §5.2).
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Long branchId;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean mustChangePassword;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user, Set<String> permissionCodes) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        this.enabled = user.isActive();
        this.accountNonLocked = !user.isAccountLocked();
        this.mustChangePassword = user.isMustChangePassword();

        List<GrantedAuthority> grantedAuthorities = permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        this.authorities = grantedAuthorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
