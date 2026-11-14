package com.pfc.thindesk.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardErrosTest {
    private DashboardService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(DashboardService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(service))
                .setControllerAdvice(new DashboardExceptionHandler()).build();
    }

    @Test
    void parametroInvalidoRetorna400SemConsultarBanco() throws Exception {
        mvc.perform(get("/api/dashboard/chamados").param("tamanho", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FILTRO_INVALIDO"));
        verifyNoInteractions(service);
    }

    @Test
    void falhaNoBancoRetorna503SemExporDetalhes() throws Exception {
        when(service.resumo(any())).thenThrow(new DataAccessResourceFailureException("segredo interno"));
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DADOS_INDISPONIVEIS"))
                .andExpect(jsonPath("$.mensagem").value("Não foi possível consultar os dados. Tente novamente."));
    }

    @Test
    void erroInesperadoRetorna500SemExporDetalhes() throws Exception {
        when(service.resumo(any())).thenThrow(new IllegalStateException("segredo interno"));
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));
    }
}
