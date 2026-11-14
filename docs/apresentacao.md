# Apresentação demonstrativa — até 3 minutos

**Situação do vídeo: pendente de gravação e publicação pelo responsável.** Após gravar, adicionar neste documento o link de acesso ou o caminho do arquivo versionado. Conferir se o avaliador consegue abrir o material. O roteiro abaixo não substitui o vídeo.

## Preparação

1. Iniciar o MongoDB e o Thindesk conforme o README.
2. Se a base estiver vazia, executar `./scripts/popular-base.ps1`.
3. Abrir o dashboard, fazer login e limpar filtros.
4. Deixar abertas uma aba com `/api/dashboard/resumo`, uma com `/api/dashboard/atendimentos?agrupamento=dia` e o resultado dos testes em docs/evidencias/resultado.md.
5. Ajustar a gravação para não mostrar outras janelas, cookies ou configurações pessoais. Gravar entre 2min40s e 2min55s para ter margem em relação ao limite.

## Roteiro sugerido — 2min50s

| Tempo | Mostrar | Fala sugerida |
| --- | --- | --- |
| 0:00–0:20 | Dashboard completo | “Este é o dashboard administrativo do Thindesk. Ele permite ao responsável acompanhar o andamento dos chamados, a demanda por tipo e as informações técnicas, utilizando dados de uma API integrada ao MongoDB.” |
| 0:20–0:50 | Cartões, gráfico e distribuição | “Os cartões mostram o total e as situações dos chamados. O gráfico apresenta as conclusões por dia ou mês, e a distribuição mostra quantidades e percentuais. Neste projeto, chamado atendido corresponde a chamado concluído.” |
| 0:50–1:20 | Filtro por tipo/técnico, aplicar e limpar | “Os filtros podem ser combinados por período, situação, tipo e técnico. O período considera a abertura; para gráfico e ranking, a conclusão também deve estar no intervalo. Aplicar atualiza os componentes e limpar retorna à visão geral.” |
| 1:20–1:40 | Ranking e tabela, expandir considerações | “O ranking mostra os tipos mais atendidos. Na tabela paginada, consultamos responsável, solicitante, datas e considerações técnicas, apoiando o acompanhamento de cada atendimento.” |
| 1:40–2:10 | Duas abas de resposta JSON | “A tela consome cinco endpoints: resumo, atendimentos, tipos, filtros e chamados. Eles exigem autenticação, validam parâmetros e utilizam agregações no MongoDB. Mantive API e tela no mesmo projeto para reaproveitar a estrutura e a sessão do Thindesk.” |
| 2:10–2:35 | Botão Atualizar e evidências | “A atualização é manual e preserva os filtros. A interface trata carregamento, ausência de dados, falhas e sessão expirada; respostas antigas não substituem a consulta atual. Os testes incluem cálculos, filtros, erros e o fluxo de criar, concluir e reabrir chamados.” |
| 2:35–2:50 | Resultado dos testes e documentos no GitHub | “Nesta revisão passaram 42 testes Java e 5 JavaScript. A documentação reúne requisitos funcionais e não funcionais, contratos dos endpoints e evidências. O resultado é um painel integrado e reproduzível para análise da operação.” |

As falas devem refletir o que estiver efetivamente publicado. Só afirmar que documentos e evidências estão no GitHub após o envio. Caso a gravação ultrapasse o tempo, reduzir a narração e manter a demonstração dos principais recursos.

## Link da gravação

Ainda não informado. Substituir esta frase pelo link do vídeo após a gravação, antes da entrega definitiva.
