package com.family.Bodeguita.household.domain;

/** Ciclo de vida de una invitación. Solo se sale de {@link #PENDING}, nunca se vuelve. */
public enum InvitationStatus {
    /** Emitida y todavía vigente: la única que se puede aceptar. */
    PENDING,
    /** Ya la usó alguien para entrar; existe el {@code User} correspondiente. */
    ACCEPTED,
    /** El OWNER se arrepintió (o se equivocó al escribir el email). */
    REVOKED,
    /**
     * Se pasó de {@code expiresAt}. Se marca de forma perezosa —al intentar aceptarla o al
     * volver a invitar ese email— para no necesitar un job que barra la tabla.
     */
    EXPIRED
}
