package com.hopestar.hfms.security.service;

import com.hopestar.hfms.module.auth.entity.RolePermission;
import com.hopestar.hfms.module.auth.entity.User;
import com.hopestar.hfms.module.auth.repository.RolePermissionRepository;
import com.hopestar.hfms.module.auth.repository.UserRepository;
import com.hopestar.hfms.security.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads a {@link User} (and its role's permission codes) for Spring
 * Security during authentication. Read-only and transactional so that the
 * lazily-fetched {@code Role} and its {@code RolePermission} collection
 * resolve safely within a single session before being mapped onto the
 * detached {@link UserPrincipal}.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("No active user found with username: " + username));

        Set<String> permissionCodes = rolePermissionRepository
                .findByRoleIdAndActiveTrue(user.getRole().getId())
                .stream()
                .map(RolePermission::getPermission)
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        return new UserPrincipal(user, permissionCodes);
    }
}
