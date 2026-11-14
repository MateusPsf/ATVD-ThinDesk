package com.pfc.thindesk.dashboard;

import static com.pfc.thindesk.dashboard.DashboardResposta.*;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/resumo")
    public Resumo resumo(@RequestParam Map<String, String> parametros) {
        return service.resumo(DashboardFiltro.de(parametros));
    }

    @GetMapping("/atendimentos")
    public Atendimentos atendimentos(@RequestParam Map<String, String> parametros) {
        return service.atendimentos(DashboardFiltro.de(parametros), parametros.get("agrupamento"));
    }

    @GetMapping("/tipos")
    public Ranking tipos(@RequestParam Map<String, String> parametros) {
        return service.tipos(DashboardFiltro.de(parametros), DashboardFiltro.inteiro(parametros, "limite", 5, 1, 20));
    }

    @GetMapping("/filtros")
    public Filtros filtros() { return service.filtros(); }

    @GetMapping("/chamados")
    public Pagina chamados(@RequestParam Map<String, String> parametros) {
        return service.chamados(DashboardFiltro.de(parametros),
                DashboardFiltro.inteiro(parametros, "pagina", 0, 0, Integer.MAX_VALUE),
                DashboardFiltro.inteiro(parametros, "tamanho", 10, 1, 100));
    }
}
