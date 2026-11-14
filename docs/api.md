# Contrato da API do Dashboard

Base local: `http://localhost:8080`. Prefixo: `/api/dashboard`. Todos os endpoints usam GET, retornam JSON e exigem a sessão autenticada do Thindesk. Não utilizam token Bearer. Após login no navegador, as URLs podem ser abertas na mesma sessão.

Os exemplos deste documento são ilustrativos e não representam necessariamente os totais da base demonstrativa. Caracteres acentuados e espaços devem ser codificados na URL; os clientes HTTP normalmente fazem isso ao enviar parâmetros.

## Autenticação para testar no PowerShell

```powershell
$null = Invoke-WebRequest http://localhost:8080/login -SessionVariable sessao
$null = Invoke-WebRequest http://localhost:8080/login -Method Post -Body @{username='admin';password='thindesk'} -WebSession $sessao
Invoke-RestMethod http://localhost:8080/api/dashboard/resumo -WebSession $sessao
```

As credenciais acima são apenas as credenciais locais de demonstração configuradas no projeto. Não versionar cookies ou credenciais de outro ambiente.

## Parâmetros comuns

Aplicam-se a resumo, atendimentos, tipos e chamados. `/filtros` retorna opções globais e não aplica esses parâmetros.

| Parâmetro | Tipo e padrão | Validação / comportamento |
| --- | --- | --- |
| dataInicio | Data opcional, sem limite inferior por padrão | YYYY-MM-DD, ano 0001–9998 |
| dataFim | Data opcional, sem limite superior por padrão | Mesmo formato; não pode anteceder dataInicio |
| status | String opcional, todas por padrão | Aberto, Em andamento, Concluído, Cancelado ou `__nao_informado__` |
| tipo | String opcional, todos por padrão | Igualdade exata após retirar espaços nas extremidades |
| tecnico | String opcional, todos por padrão | Igualdade exata após retirar espaços nas extremidades |

Campos vazios são ignorados e filtros são combinados por E. O valor reservado `__nao_informado__` seleciona os grupos sem informação. Utilizar o valor retornado em `/filtros`, não apenas o rótulo visível.

O período filtra a abertura no fuso America/Sao_Paulo. Para o gráfico e o ranking, a conclusão também precisa estar no período solicitado. Ver [regras de negócio](entrega.md).

## GET /api/dashboard/resumo

Retorna indicadores, distribuição por situação e qualidade dos dados. Aceita somente os parâmetros comuns relevantes à consulta.

Requisição:

```http
GET /api/dashboard/resumo?dataInicio=2026-11-01&dataFim=2026-11-30 HTTP/1.1
Accept: application/json
```

Resposta HTTP 200:

```json
{
  "total": 8,
  "abertos": 2,
  "emAndamento": 1,
  "concluidos": 4,
  "cancelados": 1,
  "situacoes": [
    {"status":"Aberto","quantidade":2,"percentual":25.0},
    {"status":"Em andamento","quantidade":1,"percentual":12.5},
    {"status":"Concluído","quantidade":4,"percentual":50.0},
    {"status":"Cancelado","quantidade":1,"percentual":12.5},
    {"status":"Não informado","quantidade":0,"percentual":0.0}
  ],
  "qualidade":{"semDataAbertura":0,"concluidosSemDataConclusao":0},
  "atualizadoEm":"2026-11-14T12:00:00Z"
}
```

As cinco situações aparecem mesmo zeradas. Qualidade é calculada na população filtrada: semDataAbertura conta ausência de abertura válida; concluidosSemDataConclusao conta concluídos sem data válida. Filtros de abertura excluem registros sem essa data. atualizadoEm indica quando o resumo foi gerado.

Erros possíveis: 400, 401, 503 e 500, conforme a tabela comum ao final.

## GET /api/dashboard/atendimentos

Retorna quantidade de chamados atualmente concluídos por dia ou mês. Exige data de conclusão válida.

Parâmetro adicional: `agrupamento=dia|mes`. Por padrão, usa dia se as duas datas forem informadas e o período tiver até 90 dias inclusivos; nos demais casos, usa mes.

Requisição:

```http
GET /api/dashboard/atendimentos?dataInicio=2026-11-01&dataFim=2026-11-02&agrupamento=dia HTTP/1.1
Accept: application/json
```

Resposta HTTP 200:

```json
{
  "agrupamento":"dia",
  "serie":[
    {"periodo":"2026-11-01","quantidade":0},
    {"periodo":"2026-11-02","quantidade":2}
  ]
}
```

No agrupamento mensal, periodo utiliza YYYY-MM. Intervalos conhecidos sem conclusões são preenchidos com zero. Sem período, os limites são obtidos da primeira e da última conclusão datada; sem resultados, serie é vazia. Com apenas um limite, o outro é inferido dos resultados; sem informação suficiente, serie é vazia. Com as duas datas, os intervalos são preenchidos mesmo sem resultados.

Há limite de 3660 intervalos, incluindo os inferidos. Agrupamento inválido ou série maior que o limite gera 400. Outros erros possíveis: 401, 503 e 500.

## GET /api/dashboard/tipos

Retorna os tipos dos chamados atualmente concluídos, ordenados por quantidade decrescente e nome crescente como desempate.

Parâmetro adicional: `limite`, inteiro de 1 a 20, padrão 5. Sem período, inclui concluídos sem data conhecida. Com período, exige conclusão nos limites informados. Filtrar outro status produz lista vazia.

Requisição:

```http
GET /api/dashboard/tipos?tecnico=Ana&limite=5 HTTP/1.1
Accept: application/json
```

Resposta HTTP 200:

```json
{"itens":[{"tipo":"Hardware","quantidade":3},{"tipo":"Software","quantidade":1}]}
```

Sem resultados: `{"itens":[]}`. Limite inválido gera 400; demais erros possíveis: 401, 503 e 500.

## GET /api/dashboard/filtros

Retorna opções globais, sem filtros de consulta. Status inclui sempre as quatro situações previstas e o grupo não informado. Tipos e técnicos são obtidos dos documentos; o grupo não informado aparece quando existir.

Requisição:

```http
GET /api/dashboard/filtros HTTP/1.1
Accept: application/json
```

Resposta HTTP 200:

```json
{
  "status":[
    {"valor":"Aberto","rotulo":"Aberto"},
    {"valor":"Em andamento","rotulo":"Em andamento"},
    {"valor":"Concluído","rotulo":"Concluído"},
    {"valor":"Cancelado","rotulo":"Cancelado"},
    {"valor":"__nao_informado__","rotulo":"Não informado"}
  ],
  "tipos":[{"valor":"Hardware","rotulo":"Hardware"}],
  "tecnicos":[{"valor":"Ana","rotulo":"Ana"}]
}
```

Banco vazio mantém status e retorna tipos e tecnicos vazios. Erros possíveis: 401, 503 e 500.

## GET /api/dashboard/chamados

Retorna uma página de chamados da população filtrada, com datas e considerações técnicas.

| Parâmetro adicional | Regra |
| --- | --- |
| pagina | Inteiro de 0 a 2147483647; padrão 0 |
| tamanho | Inteiro de 1 a 100; padrão 10 |

Ordenação: dataAtualizacao decrescente e id crescente como desempate; datas ausentes ao final.

Requisição:

```http
GET /api/dashboard/chamados?status=Conclu%C3%ADdo&pagina=0&tamanho=1 HTTP/1.1
Accept: application/json
```

Resposta HTTP 200:

```json
{
  "itens":[{
    "id":"exemplo-id",
    "descricao":"Computador sem conexão",
    "status":"Concluído",
    "tipo":"Hardware",
    "tecnico":"Ana",
    "usuario":"Mariana",
    "dataAbertura":"2026-11-01T12:00:00Z",
    "dataAtualizacao":"2026-11-02T15:00:00Z",
    "dataConclusao":"2026-11-02T15:00:00Z",
    "consideracoesTecnicas":"Cabo substituído e funcionamento validado."
  }],
  "pagina":0,
  "tamanho":1,
  "totalElementos":4,
  "totalPaginas":4
}
```

Datas são ISO UTC ou null. Considerações ausentes podem ser null. Uma página além do fim retorna itens vazio, preservando os totais. Nenhum resultado produz totalElementos e totalPaginas iguais a zero.

Paginação inválida gera 400; demais erros possíveis: 401, 503 e 500.

## Respostas e erros comuns

| HTTP | Código JSON | Quando ocorre |
| --- | --- | --- |
| 200 | — | Consulta válida, inclusive sem resultados |
| 400 | FILTRO_INVALIDO | Datas inválidas/invertidas, status, agrupamento, limite ou paginação inválidos |
| 401 | NAO_AUTENTICADO | Sessão ausente ou expirada |
| 503 | DADOS_INDISPONIVEIS | Falha de acesso ao MongoDB |
| 500 | ERRO_INTERNO | Falha inesperada |

Exemplo real de validação:

```http
GET /api/dashboard/resumo?dataInicio=2026-11-30&dataFim=2026-11-01 HTTP/1.1
```

```json
{"codigo":"FILTRO_INVALIDO","mensagem":"A data inicial deve ser anterior ou igual à data final."}
```

Sem sessão:

```json
{"codigo":"NAO_AUTENTICADO","mensagem":"Entre no sistema para consultar o dashboard."}
```

Falha de banco:

```json
{"codigo":"DADOS_INDISPONIVEIS","mensagem":"Não foi possível consultar os dados. Tente novamente."}
```

Falha inesperada:

```json
{"codigo":"ERRO_INTERNO","mensagem":"Não foi possível carregar o dashboard."}
```

Os erros internos são registrados no servidor sem expor seus detalhes na resposta. As respostas JSON de 500/503 são verificadas com falhas simuladas nos testes; não é necessário parar o banco de uso para demonstrá-las.
