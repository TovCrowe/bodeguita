package com.family.Bodeguita.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Si esto se rompe, invitar a alguien y que no pueda entrar deja de tener explicación. */
class EmailNormalizerTest {

    @Test
    @DisplayName("display solo quita espacios y mayúsculas: conserva lo que escribió el OWNER")
    void displayKeepsTheOriginalShape() {
        assertThat(EmailNormalizer.display("  Juan.Perez+Casa@Gmail.com "))
                .isEqualTo("juan.perez+casa@gmail.com");
    }

    @Test
    @DisplayName("en Gmail los puntos son irrelevantes: colapsan a la misma clave")
    void gmailDotsAreIrrelevant() {
        assertThat(EmailNormalizer.canonical("juan.perez@gmail.com"))
                .isEqualTo(EmailNormalizer.canonical("juanperez@gmail.com"))
                .isEqualTo("juanperez@gmail.com");
    }

    @Test
    @DisplayName("en Gmail el +tag es un alias de la misma cuenta")
    void gmailPlusTagIsAnAlias() {
        assertThat(EmailNormalizer.canonical("juan.perez+bodeguita@gmail.com"))
                .isEqualTo("juanperez@gmail.com");
    }

    @Test
    @DisplayName("googlemail.com es el mismo buzón que gmail.com")
    void googlemailIsGmail() {
        assertThat(EmailNormalizer.canonical("Juan.Perez@googlemail.com"))
                .isEqualTo("juanperez@gmail.com");
    }

    @Test
    @DisplayName("fuera de Gmail los puntos y el +tag sí distinguen buzones")
    void otherDomainsKeepEverything() {
        assertThat(EmailNormalizer.canonical("Juan.Perez+casa@example.com"))
                .isEqualTo("juan.perez+casa@example.com");
    }

    @Test
    @DisplayName("un local-part que quedaría vacío no colapsa direcciones distintas")
    void emptyLocalPartFallsBack() {
        assertThat(EmailNormalizer.canonical("+casa@gmail.com")).isEqualTo("+casa@gmail.com");
        assertThat(EmailNormalizer.canonical("+trabajo@gmail.com"))
                .isNotEqualTo(EmailNormalizer.canonical("+casa@gmail.com"));
    }

    @Test
    @DisplayName("null y cadenas sin @ no revientan: la validación del borde ya las rechazó")
    void degradesGracefully() {
        assertThat(EmailNormalizer.canonical(null)).isNull();
        assertThat(EmailNormalizer.display(null)).isNull();
        assertThat(EmailNormalizer.canonical(" NoEsUnEmail ")).isEqualTo("noesunemail");
    }
}
