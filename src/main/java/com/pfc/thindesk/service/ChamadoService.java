package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final Clock clock;

    @Autowired
    public ChamadoService(ChamadoRepository chamadoRepository) {
        this(chamadoRepository, Clock.systemUTC());
    }

    ChamadoService(ChamadoRepository chamadoRepository, Clock clock) {
        this.chamadoRepository = chamadoRepository;
        this.clock = clock;
    }

    public Chamado criarChamado(Chamado chamado) {
        if (chamado.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não informe id na criação do chamado.");
        }
        chamado.setStatus(chamado.getStatus() == null ? "Aberto" : normalizarStatus(chamado.getStatus()));
        Instant agora = clock.instant();
        chamado.setDataAbertura(agora);
        chamado.setDataAtualizacao(agora);
        chamado.setDataConclusao("Concluído".equals(chamado.getStatus()) ? agora : null);
        chamado.setTipo(limpar(chamado.getTipo()));
        chamado.setTecnico(limpar(chamado.getTecnico()));
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        return chamadoRepository.findById(id)
                .map(chamado -> {
                    String status = normalizarStatus(chamadoAtualizado.getStatus());
                    Instant agora = clock.instant();
                    if ("Concluído".equals(status)) {
                        // Preserva a ausência de data em conclusões históricas.
                        if (!statusConcluido(chamado.getStatus())) {
                            chamado.setDataConclusao(agora);
                        }
                    } else {
                        chamado.setDataConclusao(null);
                    }
                    chamado.setDescricao(chamadoAtualizado.getDescricao());
                    chamado.setStatus(status);
                    chamado.setTipo(limpar(chamadoAtualizado.getTipo()));
                    chamado.setTecnico(limpar(chamadoAtualizado.getTecnico()));
                    chamado.setUsuario(chamadoAtualizado.getUsuario());
                    chamado.setDataAtualizacao(agora);
                    if (chamadoAtualizado.getConsideracoesTecnicas() != null) {
                        chamado.setConsideracoesTecnicas(chamadoAtualizado.getConsideracoesTecnicas());
                    }
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado não encontrado."));
    }

    private static String limpar(String valor) {
        return valor == null ? null : valor.strip();
    }

    private static boolean statusConcluido(String status) {
        return status != null && List.of("concluído", "concluido").contains(status.strip().toLowerCase(Locale.ROOT));
    }

    private static String normalizarStatus(String status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o status do chamado.");
        }
        return switch (status.strip().toLowerCase(Locale.ROOT)) {
            case "aberto" -> "Aberto";
            case "em andamento", "em_andamento" -> "Em andamento";
            case "concluído", "concluido" -> "Concluído";
            case "cancelado" -> "Cancelado";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de chamado inválido.");
        };
    }

}
