package com.santosediego.donorregistry.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.santosediego.donorregistry.auth.entities.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
}
