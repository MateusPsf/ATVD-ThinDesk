package com.pfc.thindesk.dashboard;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DashboardFiltroTest {
    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "14/11/2026", "2026-1-01", "0000-01-01", "9999-01-01", "abc"})
    void rejeitaDatasInvalidas(String data) {
        assertThrows(IllegalArgumentException.class, () -> DashboardFiltro.de(Map.of("dataInicio", data)));
    }

    @Test
    void validaOrdemDasDatasEStatus() {
        assertThrows(IllegalArgumentException.class, () -> DashboardFiltro.de(Map.of("dataInicio", "2026-11-02", "dataFim", "2026-11-01")));
        assertThrows(IllegalArgumentException.class, () -> DashboardFiltro.de(Map.of("status", "Outro")));
        assertEquals(DashboardFiltro.NAO_INFORMADO, DashboardFiltro.de(Map.of("status", DashboardFiltro.NAO_INFORMADO)).status());
    }

    @Test
    void defineAgrupamentoPeloPeriodo() {
        assertEquals("mes", DashboardFiltro.de(Map.of()).agrupamento(null));
        assertEquals("dia", DashboardFiltro.de(Map.of("dataInicio", "2026-11-01", "dataFim", "2026-11-30")).agrupamento(null));
        assertEquals("mes", DashboardFiltro.de(Map.of("dataInicio", "2026-01-01", "dataFim", "2026-11-30")).agrupamento(null));
        assertThrows(IllegalArgumentException.class, () -> DashboardFiltro.de(Map.of()).agrupamento("ano"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "-1", "abc", "2147483648", ""})
    void rejeitaLimiteInvalido(String limite) {
        assertThrows(IllegalArgumentException.class, () -> DashboardFiltro.inteiro(Map.of("limite", limite), "limite", 5, 1, 20));
    }
}
