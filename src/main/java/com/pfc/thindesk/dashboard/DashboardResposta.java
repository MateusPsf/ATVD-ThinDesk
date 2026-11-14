package com.pfc.thindesk.dashboard;

import java.time.Instant;
import java.util.List;

public final class DashboardResposta {
    private DashboardResposta() {}

    public record Situacao(String status, long quantidade, double percentual) {}
    public record Qualidade(long semDataAbertura, long concluidosSemDataConclusao) {}
    public record Resumo(long total, long abertos, long emAndamento, long concluidos, long cancelados,
                         List<Situacao> situacoes, Qualidade qualidade, Instant atualizadoEm) {}
    public record Ponto(String periodo, long quantidade) {}
    public record Atendimentos(String agrupamento, List<Ponto> serie) {}
    public record Tipo(String tipo, long quantidade) {}
    public record Ranking(List<Tipo> itens) {}
    public record Opcao(String valor, String rotulo) {}
    public record Filtros(List<Opcao> status, List<Opcao> tipos, List<Opcao> tecnicos) {}
    public record ChamadoItem(String id, String descricao, String status, String tipo, String tecnico,
                              String usuario, Instant dataAbertura, Instant dataAtualizacao,
                              Instant dataConclusao, String consideracoesTecnicas) {}
    public record Pagina(List<ChamadoItem> itens, int pagina, int tamanho, long totalElementos, long totalPaginas) {}
    public record Erro(String codigo, String mensagem) {}
}
