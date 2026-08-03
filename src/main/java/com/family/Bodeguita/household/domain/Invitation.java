package com.family.Bodeguita.household.domain;

import com.family.Bodeguita.common.EmailNormalizer;
import com.family.Bodeguita.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Intención del OWNER de que alguien entre a la familia. Es lo que antes era un {@code User}
 * en estado PENDING; separarlo deja a {@code users} con personas reales y da gratis
 * caducidad, revocación y reenvío.
 *
 * <p>Solo puede haber una invitación PENDING por email en todo el sistema, lo que hace que
 * aceptar sea inequívoco: al hacer login hay como mucho una invitación que reclamar. Eso lo
 * garantiza un índice único parcial ({@code WHERE status = 'PENDING'}) que no se puede
 * expresar en JPA y vive en {@code V1__init.sql}; el service lo comprueba antes.
 */
@Entity
@Table(
        name = "household_invitation",
        indexes = @Index(name = "ix_invitation_household", columnList = "household_id"))
@Getter
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    /** Tal como lo escribió el OWNER, para poder mostrárselo en la lista de pendientes. */
    @Column(nullable = false)
    private String email;

    /** Clave con la que se busca al hacer login (ver {@link EmailNormalizer}). */
    @Column(name = "email_canonical", nullable = false)
    private String emailCanonical;

    /**
     * Auditoría: quién invitó. Solo un OWNER puede invitar, y un OWNER no se puede quitar a
     * sí mismo, así que esta FK nunca queda huérfana en el MVP.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Cuándo dejó de estar PENDING, sea por aceptación, revocación o caducidad. */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public static Invitation pending(
            Household household, User invitedBy, String email, Instant expiresAt) {
        Invitation invitation = new Invitation();
        invitation.household = household;
        invitation.invitedBy = invitedBy;
        invitation.email = EmailNormalizer.display(email);
        invitation.emailCanonical = EmailNormalizer.canonical(email);
        invitation.status = InvitationStatus.PENDING;
        invitation.expiresAt = expiresAt;
        return invitation;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean hasExpiredAt(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean belongsTo(Long householdId) {
        return household != null && household.getId() != null && household.getId().equals(householdId);
    }

    /** Reenvío: el OWNER vuelve a invitar el mismo email y la invitación gana tiempo nuevo. */
    public void renewUntil(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void accept(Instant now) {
        resolve(InvitationStatus.ACCEPTED, now);
    }

    public void revoke(Instant now) {
        resolve(InvitationStatus.REVOKED, now);
    }

    public void expire(Instant now) {
        resolve(InvitationStatus.EXPIRED, now);
    }

    private void resolve(InvitationStatus status, Instant now) {
        this.status = status;
        this.resolvedAt = now;
    }
}
