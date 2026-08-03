package com.family.Bodeguita.household.service;

import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.exception.CannotRemoveSelfException;
import com.family.Bodeguita.household.exception.HouseholdNotFoundException;
import com.family.Bodeguita.household.exception.InvitationNotFoundException;
import com.family.Bodeguita.household.exception.NotHouseholdMemberException;
import com.family.Bodeguita.household.exception.NotHouseholdOwnerException;
import com.family.Bodeguita.household.repository.HouseholdRepository;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depende de {@link UserService} e {@link InvitationService}, y nunca al revés: así no hay
 * ciclo entre los features y queda claro que household es quien manda en las membresías.
 */
@Service
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdRepository householdRepository;
    private final UserService userService;
    private final InvitationService invitationService;

    public HouseholdServiceImpl(
            HouseholdRepository householdRepository,
            UserService userService,
            InvitationService invitationService) {
        this.householdRepository = householdRepository;
        this.userService = userService;
        this.invitationService = invitationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Household getById(Long householdId) {
        return householdRepository
                .findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    @Override
    @Transactional
    public Household createWithOwner(
            String name, String googleSub, String email, String displayName, String avatarUrl) {
        Household household = householdRepository.save(new Household(name));
        userService.createOwner(household, googleSub, email, displayName, avatarUrl);
        return household;
    }

    @Override
    @Transactional
    public Invitation inviteMember(Long householdId, Long requesterId, String email) {
        User owner = requireOwner(householdId, requesterId);
        return invitationService.create(getById(householdId), owner, email);
    }

    @Override
    @Transactional
    public void revokeInvitation(Long householdId, Long requesterId, Long invitationId) {
        requireOwner(householdId, requesterId);
        Invitation invitation = invitationService.getById(invitationId);
        if (!invitation.belongsTo(householdId)) {
            throw new InvitationNotFoundException(invitationId);
        }
        invitationService.revoke(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invitation> pendingInvitations(Long householdId, Long requesterId) {
        requireOwner(householdId, requesterId);
        return invitationService.pendingFor(householdId);
    }

    @Override
    @Transactional
    public User acceptInvitation(String googleSub, String email, String name, String avatarUrl) {
        Invitation invitation = invitationService.claim(email);
        return userService.createMember(
                invitation.getHousehold(), googleSub, email, name, avatarUrl);
    }

    @Override
    @Transactional
    public void removeMember(Long householdId, Long requesterId, Long memberId) {
        requireOwner(householdId, requesterId);
        if (requesterId.equals(memberId)) {
            throw new CannotRemoveSelfException(requesterId);
        }
        User member = userService.getById(memberId);
        if (!member.belongsTo(householdId)) {
            throw new NotHouseholdMemberException(memberId, householdId);
        }
        userService.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> members(Long householdId, Long requesterId) {
        requireMember(householdId, requesterId);
        return userService.findAllByHousehold(householdId);
    }

    private User requireMember(Long householdId, Long requesterId) {
        User requester = userService.getById(requesterId);
        if (!requester.belongsTo(householdId)) {
            throw new NotHouseholdMemberException(requesterId, householdId);
        }
        return requester;
    }

    private User requireOwner(Long householdId, Long requesterId) {
        User requester = requireMember(householdId, requesterId);
        if (!requester.isOwner()) {
            throw new NotHouseholdOwnerException(requesterId, householdId);
        }
        return requester;
    }
}
