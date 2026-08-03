package com.family.Bodeguita.household.service;

import com.family.Bodeguita.common.EmailNormalizer;
import com.family.Bodeguita.household.domain.Household;
import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.domain.InvitationStatus;
import com.family.Bodeguita.household.exception.AlreadyMemberException;
import com.family.Bodeguita.household.exception.EmailAlreadyInvitedException;
import com.family.Bodeguita.household.exception.EmailBelongsToAnotherHouseholdException;
import com.family.Bodeguita.household.exception.InvitationExpiredException;
import com.family.Bodeguita.household.exception.InvitationNotFoundException;
import com.family.Bodeguita.household.exception.InvitationNotPendingException;
import com.family.Bodeguita.household.exception.NotInvitedException;
import com.family.Bodeguita.household.repository.InvitationRepository;
import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserService userService;
    private final Clock clock;
    private final Duration ttl;

    public InvitationServiceImpl(
            InvitationRepository invitationRepository,
            UserService userService,
            Clock clock,
            @Value("${bodeguita.invitation.ttl}") Duration ttl) {
        this.invitationRepository = invitationRepository;
        this.userService = userService;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    @Transactional(readOnly = true)
    public Invitation getById(Long id) {
        return invitationRepository
                .findById(id)
                .orElseThrow(() -> new InvitationNotFoundException(id));
    }

    @Override
    @Transactional
    public Invitation create(Household household, User invitedBy, String email) {
        String display = EmailNormalizer.display(email);
        requireNotAMemberYet(household, display);

        Instant now = clock.instant();
        Optional<Invitation> live =
                invitationRepository.findByEmailCanonicalAndStatus(
                        EmailNormalizer.canonical(email), InvitationStatus.PENDING);

        if (live.isPresent()) {
            Invitation existing = live.get();
            if (!existing.hasExpiredAt(now)) {
                if (!existing.belongsTo(household.getId())) {
                    throw new EmailAlreadyInvitedException(display);
                }
                // Reinvitar en la misma familia = reenviar: se le da tiempo nuevo.
                existing.renewUntil(now.plus(ttl));
                return invitationRepository.save(existing);
            }
            // Caducada: se marca ahora para liberar el único parcial y poder emitir otra.
            existing.expire(now);
            invitationRepository.save(existing);
        }

        return invitationRepository.save(
                Invitation.pending(household, invitedBy, display, now.plus(ttl)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invitation> pendingFor(Long householdId) {
        return invitationRepository.findAllByHouseholdIdAndStatus(
                householdId, InvitationStatus.PENDING);
    }

    @Override
    @Transactional
    public void revoke(Invitation invitation) {
        if (!invitation.isPending()) {
            throw new InvitationNotPendingException(invitation.getId());
        }
        invitation.revoke(clock.instant());
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public Invitation claim(String email) {
        String display = EmailNormalizer.display(email);
        Invitation invitation =
                invitationRepository
                        .findByEmailCanonicalAndStatus(
                                EmailNormalizer.canonical(email), InvitationStatus.PENDING)
                        .orElseThrow(() -> new NotInvitedException(display));

        Instant now = clock.instant();
        if (invitation.hasExpiredAt(now)) {
            invitation.expire(now);
            invitationRepository.save(invitation);
            throw new InvitationExpiredException(display);
        }

        invitation.accept(now);
        return invitationRepository.save(invitation);
    }

    /**
     * Un email que ya es un {@code User} no se puede invitar. Se distinguen los dos casos
     * porque al OWNER le sirven respuestas distintas, pero sin nombrar la otra familia.
     */
    private void requireNotAMemberYet(Household household, String display) {
        userService
                .findByEmail(display)
                .ifPresent(
                        user -> {
                            throw user.belongsTo(household.getId())
                                    ? new AlreadyMemberException(display)
                                    : new EmailBelongsToAnotherHouseholdException(display);
                        });
    }
}
