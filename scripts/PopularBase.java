import com.mongodb.client.MongoClients;
import com.mongodb.client.model.UpdateOptions;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;

/** Carga manual e repetível de dados fictícios no MongoDB local do Thindesk. */
class PopularBase {
    public static void main(String[] args) {
        popularClientes();
        if (List.of(args).contains("--somente-clientes")) return;
        String[] tipos = {"Hardware", "Software", "Rede", "Impressoras", "Acesso e permissões", "E-mail"};
        String[][] assuntos = {
            {"Computador reiniciando durante o uso", "Monitor sem imagem", "Notebook com lentidão"},
            {"Erro ao abrir aplicativo", "Atualização de sistema operacional", "Instalação de ferramenta de trabalho"},
            {"Conexão de rede instável", "Sem acesso à rede interna", "Falha no sinal Wi-Fi"},
            {"Impressora não encontrada", "Impressão com falhas", "Fila de impressão travada"},
            {"Solicitação de acesso ao sistema", "Ajuste de permissões de pasta", "Conta de usuário bloqueada"},
            {"Falha na sincronização do e-mail", "Configuração de caixa compartilhada", "Mensagens não recebidas"}
        };
        String[] tecnicos = {"Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Santos", "Elisa Costa"};
        String[] usuarios = {"Mariana Alves", "Pedro Rocha", "Juliana Freitas", "Lucas Ribeiro", "Camila Oliveira", "Rafael Martins", "Beatriz Nunes", "Felipe Castro"};
        String[] situacoes = {"Concluído", "Aberto", "Concluído", "Em andamento", "Concluído", "Cancelado", "Concluído", "Em andamento", "Concluído", "Aberto"};
        Instant agora = Instant.now();
        try (var client = MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=5000")) {
            var colecao = client.getDatabase("thindesk").getCollection("chamados");
            long antes = colecao.countDocuments();
            long inseridos = 0;
            for (int i = 0; i < 120; i++) {
                // Distribuição desigual para produzir um ranking de demanda realista.
                int tipo = new int[]{0, 1, 2, 0, 1, 3, 0, 4, 2, 5, 1, 0}[i % 12];
                String status = situacoes[i % situacoes.length];
                Instant abertura = agora.minus(Duration.ofDays((119 - i) * 89L / 119))
                        .minus(Duration.ofHours(i * 7L % 20)).minusSeconds(1800);
                if (i == 119) abertura = agora.minusSeconds(600);
                Instant atualizacao = abertura.plus(Duration.ofHours(1 + i % 48));
                if (atualizacao.isAfter(agora)) atualizacao = agora.minusSeconds(60);
                String notas = switch (status) {
                    case "Concluído" -> "Diagnóstico realizado, configuração corrigida e funcionamento validado com o solicitante. Atendimento demonstrativo.";
                    case "Em andamento" -> "Análise técnica em andamento. Testes iniciais realizados; aguardando validação da solução. Atendimento demonstrativo.";
                    case "Cancelado" -> "Solicitação cancelada após confirmação de duplicidade ou desistência do solicitante. Atendimento demonstrativo.";
                    default -> "Solicitação recebida e aguardando triagem da equipe técnica. Atendimento demonstrativo.";
                };
                String id = "demo-dashboard-v1-" + String.format("%03d", i + 1);
                Document chamado = new Document("descricao", "[DEMO] " + assuntos[tipo][(i / 12) % 3])
                        .append("status", status).append("tipo", tipos[tipo]).append("tecnico", tecnicos[(i * 3 + i / 10) % tecnicos.length])
                        .append("usuario", usuarios[(i * 3) % usuarios.length])
                        .append("dataAbertura", Date.from(abertura)).append("dataAtualizacao", Date.from(atualizacao))
                        .append("dataConclusao", "Concluído".equals(status) ? Date.from(atualizacao) : null)
                        .append("consideracoesTecnicas", notas).append("origemDados", "demo-dashboard-v1");
                var resultado = colecao.updateOne(new Document("_id", id), new Document("$setOnInsert", chamado), new UpdateOptions().upsert(true));
                if (resultado.getUpsertedId() != null) inseridos++;
            }
            System.out.println("Banco local: thindesk | Coleção: chamados");
            System.out.println("Antes: " + antes + " | Inseridos: " + inseridos + " | Total: " + colecao.countDocuments());
            System.out.println("Distribuição dos registros demonstrativos:");
            colecao.aggregate(List.of(new Document("$match", new Document("origemDados", "demo-dashboard-v1")),
                    new Document("$group", new Document("_id", "$status").append("quantidade", new Document("$sum", 1))),
                    new Document("$sort", new Document("_id", 1)))).forEach(item -> System.out.println(item.toJson()));
        }
    }

    private static void popularClientes() {
        String[] nomes = {"Mariana Alves", "Pedro Rocha", "Juliana Freitas", "Lucas Ribeiro", "Camila Oliveira", "Rafael Martins", "Beatriz Nunes", "Felipe Castro",
                "Amanda Moreira", "Gustavo Barros", "Larissa Dias", "Henrique Melo", "Fernanda Lopes", "André Cardoso", "Patrícia Ramos", "Thiago Azevedo",
                "Renata Borges", "Eduardo Campos", "Vanessa Teixeira", "Daniel Fernandes", "Letícia Pinto", "Marcelo Duarte", "Priscila Vieira", "Vinícius Correia"};
        String[] setores = {"Administrativo", "Financeiro", "Recursos Humanos", "Comercial", "Marketing", "Logística", "Atendimento", "Tecnologia da Informação"};
        try (var client = MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=5000")) {
            var colecao = client.getDatabase("thindesk").getCollection("clientes");
            long antes = colecao.countDocuments();
            long inseridos = 0;
            for (int i = 0; i < nomes.length; i++) {
                String id = "demo-clientes-v1-" + String.format("%03d", i + 1);
                Document cliente = new Document("nome", nomes[i]).append("telefone", String.format("(11) 0000-%04d", 1001 + i))
                        .append("setor", setores[i % setores.length]).append("origemDados", "demo-clientes-v1");
                var resultado = colecao.updateOne(new Document("_id", id), new Document("$setOnInsert", cliente), new UpdateOptions().upsert(true));
                if (resultado.getUpsertedId() != null) inseridos++;
            }
            System.out.println("Banco local: thindesk | Coleção: clientes");
            System.out.println("Antes: " + antes + " | Inseridos: " + inseridos + " | Total: " + colecao.countDocuments());
        }
    }
}
