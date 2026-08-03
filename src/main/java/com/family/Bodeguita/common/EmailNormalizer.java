package com.family.Bodeguita.common;

import java.util.Locale;
import java.util.Set;

/**
 * El email es la clave que enlaza una invitación con el login de Google, así que hay que
 * compararlos como los compara Google, no como cadenas.
 *
 * <p>Se guardan dos formas: la {@link #display(String)} (lo que escribió el OWNER, solo
 * saneada) y la {@link #canonical(String)}, que es la que lleva el índice único y con la
 * que se busca. Sin esto, invitar a {@code juan.perez@gmail.com} y que Google devuelva
 * {@code juanperez@gmail.com} en el login dejaría al miembro fuera sin explicación.
 */
public final class EmailNormalizer {

    /** Dominios donde los puntos del local-part son irrelevantes y {@code +tag} es un alias. */
    private static final Set<String> GOOGLE_DOMAINS = Set.of("gmail.com", "googlemail.com");

    private EmailNormalizer() {
    }

    /** Forma que se le muestra al usuario: la que escribió el OWNER, sin espacios ni mayúsculas. */
    public static String display(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Forma de comparación: única por persona real. Es la que va al índice único. */
    public static String canonical(String email) {
        String normalized = display(email);
        if (normalized == null) {
            return null;
        }
        int at = normalized.lastIndexOf('@');
        if (at < 0) {
            // La validación @Email ya rechazó esto en el borde; aquí no reventamos.
            return normalized;
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        if (!GOOGLE_DOMAINS.contains(domain)) {
            return normalized;
        }
        int plus = local.indexOf('+');
        if (plus >= 0) {
            local = local.substring(0, plus);
        }
        local = local.replace(".", "");
        // Un local-part vacío colapsaría direcciones distintas en la misma clave.
        return local.isEmpty() ? normalized : local + "@gmail.com";
    }
}
