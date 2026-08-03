package com.family.Bodeguita.user.service;

import com.family.Bodeguita.common.EmailNormalizer;
import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.dto.UpdateUserProfileRequest;
import com.family.Bodeguita.user.exception.EmailAlreadyRegisteredException;
import com.family.Bodeguita.user.exception.UserNotFoundException;
import com.family.Bodeguita.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByGoogleSub(String googleSub) {
        return userRepository.findByGoogleSub(googleSub);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailCanonical(EmailNormalizer.canonical(email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByHousehold(Long householdId) {
        return userRepository.findAllByHouseholdId(householdId);
    }

    @Override
    @Transactional
    public User createOwner(
            Household household, String googleSub, String email, String name, String avatarUrl) {
        requireFreeEmail(email);
        return userRepository.save(User.owner(household, googleSub, email, name, avatarUrl));
    }

    @Override
    @Transactional
    public User createMember(
            Household household, String googleSub, String email, String name, String avatarUrl) {
        requireFreeEmail(email);
        return userRepository.save(User.member(household, googleSub, email, name, avatarUrl));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getById(userId);
        user.setName(request.name());
        user.setAvatarUrl(request.avatarUrl());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(User user) {
        userRepository.delete(user);
    }

    /** Una persona, una familia: el email canónico es único en todo el sistema. */
    private void requireFreeEmail(String email) {
        if (userRepository.existsByEmailCanonical(EmailNormalizer.canonical(email))) {
            throw new EmailAlreadyRegisteredException(EmailNormalizer.display(email));
        }
    }
}
