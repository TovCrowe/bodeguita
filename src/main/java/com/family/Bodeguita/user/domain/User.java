package com.family.Bodeguita.user.domain;

import com.family.Bodeguita.common.EmailNormalizer;
import com.family.Bodeguita.household.domain.Household;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Miembro de una familia. Una fila aquí es siempre una persona que ya se autenticó con
 * Google: por eso {@code googleSub} no es nulo y no hay estados. Lo que todavía no es una
 * persona —una invitación sin aceptar— vive en
 * {@link com.family.Bodeguita.household.domain.Invitation}.
 *
 * <p>La tabla es {@code users} porque {@code user} es reservada en Postgres.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "ux_users_google_sub", columnNames = "google_sub"),
            @UniqueConstraint(name = "ux_users_email_canonical", columnNames = "email_canonical")
        },
        indexes = @Index(name = "ix_users_household", columnList = "household_id"))
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    /** Identificador estable de la cuenta de Google. El email puede cambiar; esto no. */
    @Column(name = "google_sub", nullable = false)
    private String googleSub;

    /** Tal como se escribió, para mostrarlo. Nunca se busca por esta columna. */
    @Column(nullable = false)
    private String email;

    /**
     * Clave de comparación (ver {@link EmailNormalizer}). Lleva el único y es por la que se
     * busca. No tiene setter propio: solo cambia junto al email, en {@link #changeEmail}.
     */
    @Column(name = "email_canonical", nullable = false)
    private String emailCanonical;

    @Setter
    @Column(length = 120)
    private String name;

    @Setter
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    // Sin JdbcTypeCode, Hibernate mapea el enum al tipo ENUM nativo del motor y
    // ddl-auto=validate no cuadraría con el VARCHAR(16) de V1__init.sql.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private User(
            Household household,
            String googleSub,
            String email,
            String name,
            String avatarUrl,
            Role role) {
        this.household = household;
        this.googleSub = googleSub;
        this.role = role;
        this.name = name;
        this.avatarUrl = avatarUrl;
        changeEmail(email);
    }

    /** Dueño de una familia recién creada: entró con Google sin invitación previa. */
    public static User owner(
            Household household, String googleSub, String email, String name, String avatarUrl) {
        return new User(household, googleSub, email, name, avatarUrl, Role.OWNER);
    }

    /** Miembro que acaba de aceptar una invitación con su primer login de Google. */
    public static User member(
            Household household, String googleSub, String email, String name, String avatarUrl) {
        return new User(household, googleSub, email, name, avatarUrl, Role.MEMBER);
    }

    /** Mantiene email y clave canónica en sincronía; nunca se tocan por separado. */
    public void changeEmail(String email) {
        this.email = EmailNormalizer.display(email);
        this.emailCanonical = EmailNormalizer.canonical(email);
    }

    public boolean belongsTo(Long householdId) {
        return household != null && household.getId() != null && household.getId().equals(householdId);
    }

    public boolean isOwner() {
        return role == Role.OWNER;
    }
}
