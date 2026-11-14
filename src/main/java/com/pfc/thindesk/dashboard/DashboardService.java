package com.pfc.thindesk.dashboard;

import static com.pfc.thindesk.dashboard.DashboardResposta.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) { this.repository = repository; }

    public Resumo resumo(DashboardFiltro filtro) {
        Map<String, Long> quantidades = new HashMap<>();
        long semAbertura = 0;
        long semConclusao = 0;
        for (Document grupo : repository.resumo(filtro)) {
            String status = grupo.getString("_id");
            quantidades.put(status, numero(grupo, "quantidade"));
            semAbertura += numero(grupo, "semDataAbertura");
            if ("Concluído".equals(status)) semConclusao += numero(grupo, "semDataConclusao");
        }
        long total = quantidades.values().stream().mapToLong(Long::longValue).sum();
        List<Situacao> situacoes = DashboardFiltro.STATUS.stream().map(status -> {
            long quantidade = quantidades.getOrDefault(status, 0L);
            double percentual = total == 0 ? 0 : Math.round(quantidade * 10000.0 / total) / 100.0;
            return new Situacao(rotulo(status), quantidade, percentual);
        }).toList();
        return new Resumo(total, quantidades.getOrDefault("Aberto", 0L), quantidades.getOrDefault("Em andamento", 0L),
                quantidades.getOrDefault("Concluído", 0L), quantidades.getOrDefault("Cancelado", 0L), situacoes,
                new Qualidade(semAbertura, semConclusao), Instant.now());
    }

    public Atendimentos atendimentos(DashboardFiltro filtro, String valorAgrupamento) {
        String agrupamento = filtro.agrupamento(valorAgrupamento);
        boolean diario = "dia".equals(agrupamento);
        LocalDate inicio = ajustar(filtro.inicio(), diario);
        LocalDate fim = ajustar(filtro.fim(), diario);
        validarIntervalos(inicio, fim, diario);
        List<Document> grupos = repository.atendimentos(filtro, agrupamento);
        Map<String, Long> valores = new HashMap<>();
        for (Document grupo : grupos) valores.put(grupo.getString("_id"), numero(grupo, "quantidade"));
        if (!grupos.isEmpty()) {
            if (inicio == null) inicio = periodo(grupos.getFirst().getString("_id"), diario);
            if (fim == null) fim = periodo(grupos.getLast().getString("_id"), diario);
        }
        if (inicio == null || fim == null) return new Atendimentos(agrupamento, List.of());
        validarIntervalos(inicio, fim, diario);
        List<Ponto> serie = new ArrayList<>();
        for (LocalDate atual = inicio; !atual.isAfter(fim); atual = diario ? atual.plusDays(1) : atual.plusMonths(1)) {
            String chave = diario ? atual.toString() : YearMonth.from(atual).toString();
            serie.add(new Ponto(chave, valores.getOrDefault(chave, 0L)));
        }
        return new Atendimentos(agrupamento, serie);
    }

    public Ranking tipos(DashboardFiltro filtro, int limite) {
        return new Ranking(repository.tipos(filtro, limite).stream()
                .map(grupo -> new Tipo(rotulo(grupo.getString("_id")), numero(grupo, "quantidade"))).toList());
    }

    public Filtros filtros() {
        Document resultado = repository.filtros();
        return new Filtros(DashboardFiltro.STATUS.stream().map(valor -> new Opcao(valor, rotulo(valor))).toList(),
                opcoes(resultado, "tipos"), opcoes(resultado, "tecnicos"));
    }

    public Pagina chamados(DashboardFiltro filtro, int pagina, int tamanho) {
        Document resultado = repository.chamados(filtro, pagina, tamanho);
        List<Document> total = documentos(resultado, "total");
        long quantidade = total.isEmpty() ? 0 : numero(total.getFirst(), "quantidade");
        List<ChamadoItem> itens = documentos(resultado, "itens").stream().map(item -> new ChamadoItem(
                item.getString("id"), item.getString("descricao"), rotulo(item.getString("status")),
                rotulo(item.getString("tipo")), rotulo(item.getString("tecnico")), item.getString("usuario"),
                instante(item, "dataAbertura"), instante(item, "dataAtualizacao"), instante(item, "dataConclusao"),
                item.getString("consideracoesTecnicas"))).toList();
        return new Pagina(itens, pagina, tamanho, quantidade, (quantidade + tamanho - 1) / tamanho);
    }

    private static List<Opcao> opcoes(Document resultado, String campo) {
        return documentos(resultado, campo).stream().map(item -> {
            String valor = item.getString("_id");
            return new Opcao(valor, rotulo(valor));
        }).toList();
    }

    private static LocalDate ajustar(LocalDate data, boolean diario) {
        return data == null || diario ? data : data.withDayOfMonth(1);
    }

    private static LocalDate periodo(String valor, boolean diario) {
        return diario ? LocalDate.parse(valor) : YearMonth.parse(valor).atDay(1);
    }

    private static void validarIntervalos(LocalDate inicio, LocalDate fim, boolean diario) {
        if (inicio != null && fim != null && (diario ? ChronoUnit.DAYS : ChronoUnit.MONTHS).between(inicio, fim) >= 3660) {
            throw new IllegalArgumentException("A série permite até 3660 intervalos. Reduza o período ou use agrupamento mensal.");
        }
    }

    private static List<Document> documentos(Document documento, String campo) {
        return documento.getList(campo, Document.class, List.of());
    }

    private static long numero(Document documento, String campo) {
        return ((Number) documento.getOrDefault(campo, 0)).longValue();
    }

    private static Instant instante(Document documento, String campo) {
        Date data = documento.getDate(campo);
        return data == null ? null : data.toInstant();
    }

    private static String rotulo(String valor) {
        return DashboardFiltro.NAO_INFORMADO.equals(valor) ? "Não informado" : valor;
    }
}
