package com.pfc.thindesk.dashboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final MongoTemplate mongo;

    public DashboardRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public List<Document> resumo(DashboardFiltro filtro) {
        List<Document> etapas = base(filtro);
        etapas.add(new Document("$group", new Document("_id", "$status")
                .append("quantidade", new Document("$sum", 1))
                .append("semDataAbertura", ausente("dataAbertura"))
                .append("semDataConclusao", ausente("dataConclusao"))));
        return executar(etapas);
    }

    public List<Document> atendimentos(DashboardFiltro filtro, String agrupamento) {
        List<Document> etapas = concluidos(filtro, true);
        Document data = new Document("format", "dia".equals(agrupamento) ? "%Y-%m-%d" : "%Y-%m")
                .append("date", "$dataConclusao").append("timezone", DashboardFiltro.FUSO.getId());
        etapas.add(new Document("$group", new Document("_id", new Document("$dateToString", data))
                .append("quantidade", new Document("$sum", 1))));
        etapas.add(new Document("$sort", new Document("_id", 1)));
        return executar(etapas);
    }

    public List<Document> tipos(DashboardFiltro filtro, int limite) {
        List<Document> etapas = concluidos(filtro, false);
        etapas.add(new Document("$group", new Document("_id", "$tipo").append("quantidade", new Document("$sum", 1))));
        etapas.add(new Document("$sort", new Document("quantidade", -1).append("_id", 1)));
        etapas.add(new Document("$limit", limite));
        return executar(etapas);
    }

    public Document filtros() {
        List<Document> etapas = new ArrayList<>();
        etapas.add(normalizar());
        etapas.add(new Document("$facet", new Document("tipos", opcoes("tipo")).append("tecnicos", opcoes("tecnico"))));
        return executar(etapas).getFirst();
    }

    public Document chamados(DashboardFiltro filtro, int pagina, int tamanho) {
        List<Document> etapas = base(filtro);
        Document campos = new Document("_id", 0).append("id", new Document("$toString", "$_id"))
                .append("descricao", 1).append("status", 1).append("tipo", 1).append("tecnico", 1)
                .append("usuario", 1).append("dataAbertura", 1).append("dataAtualizacao", 1)
                .append("dataConclusao", 1).append("consideracoesTecnicas", 1);
        List<Document> itens = List.of(
                new Document("$sort", new Document("dataAtualizacao", -1).append("_id", 1)),
                new Document("$skip", (long) pagina * tamanho), new Document("$limit", tamanho),
                new Document("$project", campos));
        etapas.add(new Document("$facet", new Document("total", List.of(new Document("$count", "quantidade"))).append("itens", itens)));
        return executar(etapas).getFirst();
    }

    private List<Document> concluidos(DashboardFiltro filtro, boolean exigirData) {
        List<Document> etapas = base(filtro);
        Document match = new Document("status", "Concluído");
        Document intervalo = intervalo(filtro);
        if (!intervalo.isEmpty()) match.append("dataConclusao", intervalo);
        else if (exigirData) match.append("dataConclusao", new Document("$ne", null));
        etapas.add(new Document("$match", match));
        return etapas;
    }

    private List<Document> base(DashboardFiltro filtro) {
        List<Document> etapas = new ArrayList<>();
        Document intervalo = intervalo(filtro);
        if (!intervalo.isEmpty()) etapas.add(new Document("$match", new Document("dataAbertura", intervalo)));
        etapas.add(normalizar());
        Document match = new Document();
        if (filtro.status() != null) match.append("status", filtro.status());
        if (filtro.tipo() != null) match.append("tipo", filtro.tipo());
        if (filtro.tecnico() != null) match.append("tecnico", filtro.tecnico());
        if (!match.isEmpty()) etapas.add(new Document("$match", match));
        return etapas;
    }

    private Document intervalo(DashboardFiltro filtro) {
        Document intervalo = new Document();
        if (filtro.inicio() != null) intervalo.append("$gte", Date.from(filtro.inicio().atStartOfDay(DashboardFiltro.FUSO).toInstant()));
        if (filtro.fim() != null) intervalo.append("$lt", Date.from(filtro.fim().plusDays(1).atStartOfDay(DashboardFiltro.FUSO).toInstant()));
        return intervalo;
    }

    private Document normalizar() {
        Document status = new Document("$toLower", texto("$status"));
        List<Document> casos = new ArrayList<>();
        casos.add(caso(status, List.of("aberto"), "Aberto"));
        casos.add(caso(status, List.of("em andamento", "em_andamento"), "Em andamento"));
        casos.add(caso(status, List.of("concluído", "concluido"), "Concluído"));
        casos.add(caso(status, List.of("cancelado"), "Cancelado"));
        return new Document("$set", new Document("status", new Document("$switch",
                new Document("branches", casos).append("default", DashboardFiltro.NAO_INFORMADO)))
                .append("tipo", grupo(new Document("$ifNull", List.of("$tipo", "$Tipo"))))
                .append("tecnico", grupo("$tecnico"))
                .append("dataAbertura", data("$dataAbertura"))
                .append("dataAtualizacao", data("$dataAtualizacao"))
                .append("dataConclusao", data("$dataConclusao")));
    }

    private static Document caso(Document status, List<String> valores, String resultado) {
        return new Document("case", new Document("$in", List.of(status, valores))).append("then", resultado);
    }

    private static Document texto(Object campo) {
        return new Document("$trim", new Document("input", new Document("$convert",
                new Document("input", campo).append("to", "string").append("onNull", "").append("onError", ""))));
    }

    private static Document grupo(Object campo) {
        Document texto = texto(campo);
        return new Document("$cond", List.of(new Document("$eq", List.of(texto, "")), DashboardFiltro.NAO_INFORMADO, texto));
    }

    private static Document data(String campo) {
        return new Document("$cond", List.of(new Document("$eq", List.of(new Document("$type", campo), "date")), campo,
                new Document("$literal", null)));
    }

    private static Document ausente(String campo) {
        return new Document("$sum", new Document("$cond", List.of(new Document("$eq", List.of("$" + campo,
                new Document("$literal", null))), 1, 0)));
    }

    private static List<Document> opcoes(String campo) {
        return List.of(new Document("$group", new Document("_id", "$" + campo)), new Document("$sort", new Document("_id", 1)));
    }

    private List<Document> executar(List<Document> etapas) {
        List<AggregationOperation> operacoes = etapas.stream().<AggregationOperation>map(etapa -> context -> etapa).toList();
        return mongo.aggregate(Aggregation.newAggregation(operacoes), "chamados", Document.class).getMappedResults();
    }
}
