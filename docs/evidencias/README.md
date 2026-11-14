# Evidências da entrega

## Testes automatizados

- [Resultado e ambiente da execução](resultado.md).
- [Log Java completo](java.log): 42 testes, incluindo 12 de integração, sem falhas.
- [Log JavaScript](javascript.log): 5 testes aprovados.
- Relatórios por classe: [erros HTTP](com.pfc.thindesk.dashboard.DashboardErrosTest.txt), [filtros](com.pfc.thindesk.dashboard.DashboardFiltroTest.txt), [integração](com.pfc.thindesk.dashboard.DashboardIntegracaoTest.txt) e [ciclo de vida](com.pfc.thindesk.service.ChamadoServiceTest.txt).

Reprodução: executar `./scripts/validar-entrega.ps1` na pasta do projeto com MongoDB local ativo. O script salva uma nova execução dos testes sem alterar o banco de demonstração.

Os testes de integração conferem autenticação, os cinco endpoints, cálculos, ranking, limites de datas e fuso, paginação, campos antigos, banco vazio, renderização da página e o fluxo de criação, conclusão e reabertura. Os testes JavaScript verificam atualização, preservação de filtros, falha parcial, recuperação, sessão expirada e descarte de respostas antigas.

As falhas 500 e 503 são simuladas nos testes negativos: exceções correspondentes no log são esperadas. A verificação JavaScript utiliza um DOM mínimo simulado; a conferência visual é registrada separadamente.

## Consultas reais da API

[Respostas capturadas em JSON](consultas-api.json), com método, caminho, código HTTP e corpo. Foram consultados os cinco endpoints, um filtro por tipo, um erro 400 de datas invertidas e um erro 401 sem sessão. Cookies e credenciais não foram salvos.

Na captura, o banco demonstrativo retornou 120 chamados: 60 concluídos, 24 abertos, 24 em andamento e 12 cancelados. O filtro Hardware retornou 40 chamados. Esses valores correspondem ao instante da captura; outras execuções podem refletir mudanças na base.

## Conferência visual

Capturas do sistema em execução no endereço local, com dados fictícios:

- [Dashboard em computador](dashboard-desktop.png): indicadores, gráfico diário, situação, ranking e lista com considerações.
- [Dashboard em celular](dashboard-celular.png): largura de 390 pixels; a página não apresentou rolagem horizontal global. A tabela utiliza sua própria rolagem.

O filtro de tipo foi aplicado e depois limpo no navegador. A captura foi realizada com a consulta geral restaurada. Não foram observados erros no console durante essa conferência.

![Dashboard com dados demonstrativos](dashboard-desktop.png)

## Versionamento

[Histórico Git verificado](historico-git.txt). A referência remota foi atualizada em leitura; a main contém o merge da implementação. Estes novos documentos e relatórios ainda precisam ser incluídos em commit e enviados pelo responsável.

As evidências não representam auditoria de segurança, certificação de acessibilidade ou teste de carga.
