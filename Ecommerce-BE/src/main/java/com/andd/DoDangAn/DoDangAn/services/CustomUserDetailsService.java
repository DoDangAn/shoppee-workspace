package com.andd.DoDangAn.DoDangAn.services;

import com.andd.DoDangAn.DoDangAn.models.User;
import com.andd.DoDangAn.DoDangAn.repository.jpa.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        Hibernate.initialize(user.getAuthorities());
        return user;
    }

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}