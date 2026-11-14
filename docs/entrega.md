# Documento de entrega — Dashboard Administrativo do Thindesk

## 1. Contexto e objetivo

O responsável pelos chamados precisa acompanhar o andamento dos atendimentos, consultar situações e considerações técnicas e identificar os serviços de maior demanda. A solução integra uma tela administrativa ao Thindesk e desenvolve a API que fornece seus indicadores, séries temporais, rankings e filtros.

Ator principal: usuário autenticado responsável pelo acompanhamento da operação. A versão atual utiliza a autenticação existente, sem um novo perfil exclusivo de administrador.

## 2. Requisitos funcionais

| ID | Requisito e critério de aceite | Implementação |
| --- | --- | --- |
| RF01 | Apresentar total de chamados e quantidades por situação, respeitando os filtros | Cartões e GET `/api/dashboard/resumo` |
| RF02 | Analisar chamados atendidos por dia ou mês, incluindo intervalos zerados | Gráfico SVG, tabela alternativa e GET `/api/dashboard/atendimentos` |
| RF03 | Apresentar situações com quantidades e percentuais, incluindo registros sem informação | Distribuição e legenda; cinco situações no resumo |
| RF04 | Mostrar tipos mais atendidos em ordem decrescente de quantidade | Ranking Top 5 e GET `/api/dashboard/tipos`, com desempate pelo nome |
| RF05 | Consultar por datas, situação, tipo e técnico combinados; permitir limpar filtros | Formulário, validação e parâmetros comuns da API |
| RF06 | Consumir dados do banco pela API REST, utilizando a sessão do sistema | Cinco endpoints autenticados e requisições fetch da tela |
| RF07 | Atualizar e tratar dados: carregamento, vazio, falhas, sessão expirada e dados antigos | Botão Atualizar, mensagens, tempo limite, qualidade e descarte de respostas antigas |

Recurso complementar: lista paginada com descrição, situação, tipo, técnico, solicitante, datas e considerações técnicas, fornecida por `/api/dashboard/chamados`.

## 3. Requisitos não funcionais

| ID | Requisito verificável | Evidência / limite |
| --- | --- | --- |
| RNF01 — Controle de acesso | Exigir sessão nos endpoints e na página; API sem sessão retorna 401 JSON e página solicita login | Testes de integração com autenticação real por formulário |
| RNF02 — Usabilidade e adaptação | Manter filtros identificados, mensagens de estado e layout utilizável em computador e celular | Conferência visual; tabela tem rolagem própria em telas pequenas |
| RNF03 — Acesso ao conteúdo | Fornecer alternativa tabular ao gráfico, foco visível e mensagens anunciáveis | HTML com labels, tabela alternativa e aria-live; não equivale a auditoria de acessibilidade |
| RNF04 — Integridade | Gerenciar datas pelo servidor, preservar abertura e considerações omitidas; tratar reabertura sem inventar histórico | Testes de ChamadoService e fluxo integrado criar/concluir/reabrir |
| RNF05 — Eficiência | Executar agregações e paginação no MongoDB; limitar ranking, página e extensão da série | Limites: ranking 20, página 100 e série 3660 intervalos; não há SLA nem teste de carga |
| RNF06 — Recuperação | Não substituir uma consulta atual por resposta antiga; preservar dados anteriores com aviso se uma carga falhar | Testes JavaScript de concorrência, falha parcial e recuperação |
| RNF07 — Manutenção | Separar controller, serviço, repositório e respostas; documentar contratos e ter testes reproduzíveis | Pacote dashboard, documentos e script validar-entrega.ps1 |
| RNF08 — Execução reproduzível | Documentar versões, configuração e carga demonstrativa; evitar dependência de CDN nos novos gráficos | README, Maven, scripts de carga e CSS/SVG locais |
| RNF09 — Tratamento de conteúdo | Renderizar nomes, descrições e observações como texto, sem executar HTML recebido | Uso de textContent e conferência de observação contendo tag de script |

## 4. Arquitetura e decisões

Fluxo principal: **navegador → DashboardController → DashboardService → DashboardRepository → MongoDB**. O JSON retorna ao navegador, que renderiza os cartões, gráficos e tabela.

- Spring Boot mantém a API e as páginas no mesmo servidor. Isso reaproveita a sessão do Thindesk e evita criar outra aplicação para a atividade.
- Thymeleaf entrega a estrutura da tela; JavaScript consulta os endpoints com fetch. A tela não calcula indicadores a partir de uma lista completa de registros.
- MongoDB agrega, ordena e pagina os resultados. Índices por abertura e por status/conclusão auxiliam consultas; a normalização de dados antigos ainda exige processamento.
- DTOs em records definem respostas específicas da API, sem expor a entidade inteira nos indicadores.
- SVG e CSS produzem os gráficos localmente, sem instalar biblioteca de gráficos.
- A atualização é manual: reduz requisições desnecessárias e permite ao usuário escolher quando recarregar os dados. Atualização automática não foi exigida pelo enunciado apresentado.

## 5. Modelo de dados

### Coleção chamados

| Campo | Tipo | Regra |
| --- | --- | --- |
| id | String | Identificador do documento |
| descricao | String | Descrição do problema |
| status | String | Aberto, Em andamento, Concluído ou Cancelado nas novas gravações |
| tipo | String | Categoria do atendimento |
| tecnico | String | Responsável pelo atendimento |
| usuario | String | Nome do solicitante |
| dataAbertura | Instant / BSON Date | Gerada na criação, preservada na atualização |
| dataAtualizacao | Instant / BSON Date | Atualizada pelo servidor em cada gravação |
| dataConclusao | Instant / BSON Date ou null | Registrada ao concluir e limpa ao sair de concluído |
| consideracoesTecnicas | String ou null | Diagnóstico, solução e observações em texto |

Uma edição de chamado já concluído preserva sua data de conclusão. Um registro antigo já concluído e sem data permanece sem data histórica até que seja reaberto e concluído novamente. A API de atualização preserva considerações ausentes ou nulas e aceita string vazia para limpar o texto.

### Coleção clientes

Campos: id, nome, telefone e setor. O solicitante do chamado é atualmente armazenado como texto em usuario; não existe chave estrangeira ou vínculo obrigatório com Cliente. As cargas demonstrativas utilizam nomes compatíveis, mas não criam uma relação referencial.

Os scripts adicionam origemDados apenas como identificação dos registros demonstrativos. Esse campo não altera as regras do dashboard.

## 6. Regras de negócio da análise

1. Chamado “atendido” significa chamado atualmente concluído. A versão não conta várias conclusões históricas de um mesmo chamado reaberto.
2. Datas são armazenadas em UTC e apresentadas no fuso America/Sao_Paulo.
3. O período seleciona a data de abertura; dia inicial e final são inclusivos na interface. No banco, o limite superior é o início do dia seguinte, exclusivo.
4. Gráfico e ranking consideram concluídos dessa população; havendo período, exigem também conclusão dentro dos limites informados. Essa regra está explicada na tela.
5. Sem período, o ranking pode incluir concluídos sem data; o gráfico exige data de conclusão válida.
6. Campos vazios são agrupados como Não informado; status desconhecidos também entram nesse grupo. O filtro utiliza o valor reservado `__nao_informado__`.
7. Grafias antigas conhecidas de status e o campo legado Tipo são normalizados na consulta, sem alterar os documentos.
8. Percentual = quantidade / total filtrado × 100, com duas casas. Base vazia produz zero; arredondamentos podem impedir soma exata de 100.
9. Filtros são combinados por E. As opções de situação, tipo e técnico são globais, para não desaparecerem após uma consulta vazia.
10. Endpoint e horário de atualização não representam uma transação única: alterações simultâneas no banco podem aparecer entre consultas diferentes.

## 7. API e testes

O [contrato da API](api.md) contém os endpoints, parâmetros, exemplos de requisição/resposta e erros.

As [evidências reproduzíveis](evidencias/README.md) apresentam os resultados desta revisão. A integração utiliza um banco temporário de nome aleatório e não remove dados do banco de demonstração. Os testes JavaScript usam um DOM simulado para testar o fluxo; a captura de tela complementa a verificação.

## 8. Limites e pendências externas

- O projeto é uma entrega acadêmica local. A autenticação usa usuário/senha de demonstração; a configuração herdada mantém CSRF desativado. Não foi realizada preparação para exposição pública, auditoria de segurança, teste de carga ou auditoria completa de acessibilidade.
- Os botões de cadastro/edição de páginas antigas não representam um CRUD completo desenvolvido nesta atividade. O escopo entregue é o dashboard, sua API e o ciclo de vida necessário aos indicadores.
- A carga demonstrativa é manual e os dados do MongoDB não acompanham o Git.
- A [apresentação](apresentacao.md) possui roteiro; o vídeo de até três minutos ainda precisa ser gravado e seu link disponibilizado.
- Commits, publicação no GitHub, pull requests e merge não são executados automaticamente. Consulte a [situação do versionamento](versionamento.md).
