package com.pfc.thindesk.dashboard;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "testes.mongo", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardIntegracaoTest {
    // Este banco é exclusivo do teste: o banco de uso do sistema nunca é limpo.
    private static final String BANCO = "thindesk_teste_dashboard_" + UUID.randomUUID().toString().replace("-", "");
    @Autowired private MockMvc mvc;
    @Autowired private MongoTemplate mongo;
    private MockHttpSession sessao;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("spring.data.mongodb.uri", () -> "mongodb://127.0.0.1:27017/" + BANCO + "?serverSelectionTimeoutMS=3000");
        registro.add("spring.data.mongodb.database", () -> BANCO);
        registro.add("spring.security.user.name", () -> "admin");
        registro.add("spring.security.user.password", () -> "thindesk");
    }

    @BeforeEach
    void preparar() throws Exception {
        assertEquals(BANCO, mongo.getDb().getName());
        mongo.getCollection("chamados").deleteMany(new Document());
        inserir("a", "Aberto", " Hardware ", " Ana ", "2026-11-01T03:00:00Z", null);
        inserir("b", "EM_ANDAMENTO", "Software", "Bruno", "2026-11-01T12:00:00Z", null);
        inserir("c", "Concluído", "Hardware", "Ana", "2026-11-01T04:00:00Z", "2026-11-02T03:00:00Z");
        inserir("d", "CONCLUIDO", "Software", "Ana", "2026-11-02T03:00:00Z", "2026-11-03T02:59:59.999Z");
        inserir("e", "Concluído", "Hardware", "Bruno", "2026-10-31T12:00:00Z", "2026-11-02T12:00:00Z");
        inserir("f", "Cancelado", "Software", "Bruno", "2026-11-03T03:00:00Z", null);
        inserir("g", "Desconhecido", " ", null, null, null);
        inserir("h", "CONCLUIDO", null, null, null, null);
        mongo.getCollection("chamados").updateOne(new Document("_id", "h"), new Document("$set", new Document("Tipo", "Rede")));
        inserir("i", "Concluído", "Hardware", "Ana", "2026-11-02T05:00:00Z", "2026-11-03T03:00:00Z");
        sessao = (MockHttpSession) mvc.perform(post("/login").param("username", "admin").param("password", "thindesk"))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
        assertNotNull(sessao);
    }

    @AfterAll
    void limparBancoExclusivo() {
        assertEquals(BANCO, mongo.getDb().getName());
        mongo.getDb().drop();
    }

    @Test
    void exigeLoginEmTodosOsEndpointsSemRedirecionar() throws Exception {
        for (String caminho : new String[]{"resumo", "atendimentos", "tipos", "filtros", "chamados"}) {
            mvc.perform(get("/api/dashboard/" + caminho)).andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        }
    }

    @Test
    void dashboardRenderizaPaginaProtegidaERecursosLocais() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"filtros-form\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-api=\"/api/dashboard/\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/dashboard.js")));
        mvc.perform(get("/js/dashboard.js")).andExpect(status().isOk());
        mvc.perform(get("/css/dashboard.css")).andExpect(status().isOk());
    }

    @Test
    void criacaoConclusaoEReaberturaAtualizamDashboard() throws Exception {
        String aberto = """
                {"descricao":"Teste do fluxo completo","status":"Aberto","tipo":"Fluxo de validação",
                 "tecnico":"Ana","usuario":"Teste","consideracoesTecnicas":"Diagnóstico inicial"}
                """;
        String criado = mvc.perform(post("/api/chamados").session(sessao).contentType("application/json").content(aberto))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(criado).get("id").asText();
        mvc.perform(get("/api/dashboard/resumo").session(sessao).param("tipo", "Fluxo de validação"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.abertos").value(1));
        String concluido = aberto.replace("\"Aberto\"", "\"Concluído\"").replace("Diagnóstico inicial", "Solução aplicada");
        mvc.perform(put("/api/chamados/" + id).session(sessao).contentType("application/json").content(concluido))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataConclusao").isString());
        mvc.perform(get("/api/dashboard/resumo").session(sessao).param("tipo", "Fluxo de validação"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.concluidos").value(1)).andExpect(jsonPath("$.abertos").value(0));
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao).param("tipo", "Fluxo de validação").param("agrupamento", "dia"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serie[0].quantidade").value(1));
        mvc.perform(get("/api/dashboard/chamados").session(sessao).param("tipo", "Fluxo de validação"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].consideracoesTecnicas").value("Solução aplicada"));
        mvc.perform(put("/api/chamados/" + id).session(sessao).contentType("application/json").content(aberto))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataConclusao").isEmpty());
        mvc.perform(get("/api/dashboard/tipos").session(sessao).param("tipo", "Fluxo de validação"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void calculaResumoEQualidadeIncluindoLegados() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(9)).andExpect(jsonPath("$.abertos").value(1))
                .andExpect(jsonPath("$.emAndamento").value(1)).andExpect(jsonPath("$.concluidos").value(5))
                .andExpect(jsonPath("$.cancelados").value(1)).andExpect(jsonPath("$.situacoes.length()").value(5))
                .andExpect(jsonPath("$.situacoes[2].percentual").value(55.56))
                .andExpect(jsonPath("$.situacoes[4].quantidade").value(1))
                .andExpect(jsonPath("$.qualidade.semDataAbertura").value(2))
                .andExpect(jsonPath("$.qualidade.concluidosSemDataConclusao").value(1))
                .andExpect(jsonPath("$.atualizadoEm").isString());
    }

    @Test
    void periodoUsaFusoLocalEPreencheZeros() throws Exception {
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao)
                .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agrupamento").value("dia"))
                .andExpect(jsonPath("$.serie.length()").value(2))
                .andExpect(jsonPath("$.serie[0].periodo").value("2026-11-01"))
                .andExpect(jsonPath("$.serie[0].quantidade").value(0))
                .andExpect(jsonPath("$.serie[1].quantidade").value(2));
        mvc.perform(get("/api/dashboard/resumo").session(sessao)
                .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.concluidos").value(3));
    }

    @Test
    void rankingOrdenaEmpatesELimitaResultados() throws Exception {
        mvc.perform(get("/api/dashboard/tipos").session(sessao).param("limite", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].tipo").value("Hardware"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3));
        mvc.perform(get("/api/dashboard/tipos").session(sessao)
                .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].tipo").value("Hardware"))
                .andExpect(jsonPath("$.itens[1].tipo").value("Software"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(1));
    }

    @Test
    void combinaFiltrosEConsultaGrupoNaoInformado() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").session(sessao).param("status", "Concluído")
                .param("tipo", " Hardware ").param("tecnico", "Ana"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/dashboard/chamados").session(sessao).param("status", DashboardFiltro.NAO_INFORMADO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("Não informado"))
                .andExpect(jsonPath("$.itens[0].tipo").value("Não informado"));
        mvc.perform(get("/api/dashboard/filtros").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.length()").value(5))
                .andExpect(jsonPath("$.tipos.length()").value(4))
                .andExpect(jsonPath("$.tecnicos.length()").value(3));
    }

    @Test
    void paginaChamadosComConsideracoesEDatasIso() throws Exception {
        mvc.perform(get("/api/dashboard/chamados").session(sessao).param("tamanho", "2").param("pagina", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(9))
                .andExpect(jsonPath("$.totalPaginas").value(5)).andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].id").value("d"))
                .andExpect(jsonPath("$.itens[0].dataAbertura").value("2026-11-02T03:00:00Z"))
                .andExpect(jsonPath("$.itens[0].consideracoesTecnicas").value("Diagnóstico d"));
        mvc.perform(get("/api/dashboard/chamados").session(sessao).param("pagina", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void consultaVaziaRetornaZeroEMesesSemAtendimento() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").session(sessao).param("tipo", "Inexistente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.situacoes[0].percentual").value(0));
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao).param("tipo", "Inexistente")
                .param("dataInicio", "2026-10-01").param("dataFim", "2026-12-31").param("agrupamento", "mes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serie.length()").value(3))
                .andExpect(jsonPath("$.serie[1].periodo").value("2026-11"))
                .andExpect(jsonPath("$.serie[1].quantidade").value(0));
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao).param("tipo", "Inexistente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serie").isEmpty());
    }

    @Test
    void rejeitaParametrosAntesDaConsulta() throws Exception {
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao).param("agrupamento", "ano"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FILTRO_INVALIDO"));
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao).param("agrupamento", "dia")
                .param("dataInicio", "2000-01-01").param("dataFim", "2026-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dashboard/tipos").session(sessao).param("limite", "21"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dashboard/resumo").session(sessao).param("dataInicio", "2026-02-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bancoVazioMantemContratoDosCincoEndpoints() throws Exception {
        mongo.getCollection("chamados").deleteMany(new Document());
        mvc.perform(get("/api/dashboard/resumo").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.situacoes.length()").value(5))
                .andExpect(jsonPath("$.qualidade.semDataAbertura").value(0));
        mvc.perform(get("/api/dashboard/filtros").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.length()").value(5)).andExpect(jsonPath("$.tipos").isEmpty())
                .andExpect(jsonPath("$.tecnicos").isEmpty());
        mvc.perform(get("/api/dashboard/chamados").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty()).andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
        mvc.perform(get("/api/dashboard/tipos").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty());
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.serie").isEmpty());
    }

    @Test
    void serieSemPeriodoUsaConclusoesDatadasEMesesVaziosSaoPreenchidos() throws Exception {
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.agrupamento").value("mes"))
                .andExpect(jsonPath("$.serie.length()").value(1))
                .andExpect(jsonPath("$.serie[0].quantidade").value(4));
        mvc.perform(get("/api/dashboard/atendimentos").session(sessao)
                .param("dataInicio", "2026-10-01").param("dataFim", "2026-12-31").param("agrupamento", "mes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.serie.length()").value(3))
                .andExpect(jsonPath("$.serie[0].quantidade").value(0))
                .andExpect(jsonPath("$.serie[1].quantidade").value(4))
                .andExpect(jsonPath("$.serie[2].quantidade").value(0));
        mvc.perform(get("/api/dashboard/tipos").session(sessao).param("status", "Aberto"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty());
    }

    private void inserir(String id, String status, String tipo, String tecnico, String abertura, String conclusao) {
        Document chamado = new Document("_id", id).append("descricao", "Chamado " + id).append("status", status)
                .append("tecnico", tecnico).append("usuario", "Usuário").append("consideracoesTecnicas", "Diagnóstico " + id);
        if (tipo != null) chamado.append("tipo", tipo);
        if (abertura != null) {
            chamado.append("dataAbertura", Date.from(Instant.parse(abertura)))
                    .append("dataAtualizacao", Date.from(Instant.parse(abertura).plusSeconds(600)));
        }
        if (conclusao != null) chamado.append("dataConclusao", Date.from(Instant.parse(conclusao)));
        mongo.getCollection("chamados").insertOne(chamado);
    }
}
