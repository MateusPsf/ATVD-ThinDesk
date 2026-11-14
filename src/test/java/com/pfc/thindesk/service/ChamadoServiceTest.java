package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChamadoServiceTest {
    private static final Instant AGORA = Instant.parse("2026-11-13T18:00:00Z");
    private ChamadoRepository repository;
    private ChamadoService service;

    @BeforeEach
    void preparar() {
        repository = mock(ChamadoRepository.class);
        service = new ChamadoService(repository, Clock.fixed(AGORA, ZoneOffset.UTC));
        when(repository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void criacaoUsaDatasDoServidorEStatusPadrao() {
        Chamado novo = new Chamado();
        novo.setDataAbertura(Instant.EPOCH);
        novo.setDataConclusao(Instant.EPOCH);
        novo.setTipo(" Manutenção ");
        novo.setTecnico(" João ");
        novo.setConsideracoesTecnicas("Diagnóstico inicial");
        Chamado salvo = service.criarChamado(novo);
        assertEquals("Aberto", salvo.getStatus());
        assertEquals(AGORA, salvo.getDataAbertura());
        assertEquals(AGORA, salvo.getDataAtualizacao());
        assertNull(salvo.getDataConclusao());
        assertEquals("Manutenção", salvo.getTipo());
        assertEquals("João", salvo.getTecnico());
        assertEquals("Diagnóstico inicial", salvo.getConsideracoesTecnicas());
    }

    @ParameterizedTest
    @CsvSource({"ABERTO,Aberto", "EM_ANDAMENTO,Em andamento", "CONCLUIDO,Concluído", "Cancelado,Cancelado"})
    void aceitaValoresDoFormularioExistente(String entrada, String esperado) {
        Chamado novo = atualizacao(entrada);
        service.criarChamado(novo);
        assertEquals(esperado, novo.getStatus());
        assertEquals("Concluído".equals(esperado) ? AGORA : null, novo.getDataConclusao());
    }

    @Test
    void conclusaoPreservaAberturaEIgnoraDatasEnviadas() {
        Chamado existente = existente("Aberto");
        existente.setDataAbertura(Instant.EPOCH);
        Chamado entrada = atualizacao("CONCLUIDO");
        entrada.setDataAbertura(AGORA);
        entrada.setDataAtualizacao(Instant.EPOCH);
        entrada.setDataConclusao(Instant.EPOCH);
        service.atualizarChamado("1", entrada);
        assertEquals(Instant.EPOCH, existente.getDataAbertura());
        assertEquals(AGORA, existente.getDataAtualizacao());
        assertEquals(AGORA, existente.getDataConclusao());
    }

    @Test
    void editarConclusaoPreservaDataEConsideracoesAusentes() {
        Chamado existente = existente("Concluído");
        existente.setDataConclusao(Instant.EPOCH);
        existente.setConsideracoesTecnicas("Cabo substituído");
        service.atualizarChamado("1", atualizacao("Concluído"));
        assertEquals(Instant.EPOCH, existente.getDataConclusao());
        assertEquals("Cabo substituído", existente.getConsideracoesTecnicas());
        Chamado entrada = atualizacao("Concluído");
        entrada.setConsideracoesTecnicas("");
        service.atualizarChamado("1", entrada);
        assertEquals("", existente.getConsideracoesTecnicas());
    }

    @Test
    void reaberturaLimpaConclusaoEPermiteNovaConclusao() {
        Chamado existente = existente("Concluído");
        existente.setDataConclusao(Instant.EPOCH);
        service.atualizarChamado("1", atualizacao("Em andamento"));
        assertNull(existente.getDataConclusao());
        service.atualizarChamado("1", atualizacao("Concluído"));
        assertEquals(AGORA, existente.getDataConclusao());
    }

    @Test
    void legadoConcluidoNaoRecebeDatasHistoricasInventadas() {
        Chamado existente = existente("CONCLUIDO");
        service.atualizarChamado("1", atualizacao("Concluído"));
        assertNull(existente.getDataAbertura());
        assertNull(existente.getDataConclusao());
        assertEquals(AGORA, existente.getDataAtualizacao());
    }

    @Test
    void cancelamentoLimpaConclusao() {
        Chamado existente = existente("Concluído");
        existente.setDataConclusao(Instant.EPOCH);
        service.atualizarChamado("1", atualizacao("Cancelado"));
        assertNull(existente.getDataConclusao());
    }

    @Test
    void rejeitaStatusInvalidoSemAlterarOuSalvar() {
        Chamado existente = existente("Aberto");
        existente.setDescricao("Original");
        Chamado entrada = atualizacao("Inexistente");
        entrada.setDescricao("Alterada");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.atualizarChamado("1", entrada)).getStatusCode());
        assertEquals("Original", existente.getDescricao());
        verify(repository, never()).save(any());
        assertThrows(ResponseStatusException.class, () -> service.criarChamado(atualizacao("Inválido")));
        assertThrows(ResponseStatusException.class, () -> service.atualizarChamado("1", new Chamado()));
    }

    @Test
    void criacaoComIdNaoSobrescreveRegistroExistente() {
        Chamado novo = atualizacao("Aberto");
        novo.setId("1");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.criarChamado(novo)).getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void idInexistenteRetornaNaoEncontrado() {
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> service.atualizarChamado("ausente", atualizacao("Aberto"))).getStatusCode());
        verify(repository, never()).save(any());
    }

    private Chamado existente(String status) {
        Chamado chamado = atualizacao(status);
        chamado.setId("1");
        when(repository.findById("1")).thenReturn(Optional.of(chamado));
        return chamado;
    }

    private Chamado atualizacao(String status) {
        Chamado chamado = new Chamado();
        chamado.setStatus(status);
        return chamado;
    }
}
