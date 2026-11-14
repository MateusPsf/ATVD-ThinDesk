# Thindesk — Dashboard Administrativo

Painel integrado ao sistema de chamados para acompanhar atendimentos técnicos e apoiar decisões com indicadores, gráficos, ranking e filtros. A tela consome uma API REST desenvolvida no próprio projeto e integrada ao MongoDB.

## Documentos da entrega

- [Requisitos funcionais e não funcionais, modelo de dados e decisões de projeto](docs/entrega.md).
- [API: endpoints, parâmetros, requisições, respostas e erros](docs/api.md).
- [Evidências de testes e conferência visual](docs/evidencias/README.md).
- [Roteiro do vídeo de até três minutos](docs/apresentacao.md).
- [Situação do Gitflow e passos para entrega no GitHub](docs/versionamento.md).

## Recursos

| Requisito | Recurso |
| --- | --- |
| RF01 | Total de chamados, abertos, em andamento, concluídos e cancelados |
| RF02 | Gráfico de conclusões por dia ou mês, com tabela alternativa |
| RF03 | Distribuição por situação, quantidade e percentual |
| RF04 | Ranking dos tipos mais atendidos |
| RF05 | Período, situação, tipo e técnico combinados |
| RF06 | Integração autenticada com cinco endpoints da API |
| RF07 | Atualização manual, carregamento, vazio, qualidade e tratamento de falhas |

A lista paginada permite consultar descrição, técnico, solicitante, datas e considerações técnicas. O layout se adapta a computador e celular.

## Executar

Requisitos: JDK 23, Maven e MongoDB ativo em `127.0.0.1:27017`. Node.js é utilizado somente nos testes JavaScript.

Na pasta do projeto:

```powershell
mvn spring-boot:run
```

Se o Maven não estiver no PATH desta máquina, o executável instalado pode ser usado diretamente:

```powershell
& 'C:\Users\Remakker\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```

Abrir [o dashboard](http://localhost:8080/dashboard) e entrar com usuário **admin** e senha **thindesk**, credenciais de demonstração. Encerrar com Ctrl+C. Reiniciar instâncias antigas para carregar mudanças.

O banco local padrão é `thindesk`. Para usar outra conexão, definir `MONGODB_URI` e `MONGODB_DATABASE`. Dados de um banco remoto não são importados automaticamente. Esta é uma configuração acadêmica/local; veja as limitações de segurança no documento da entrega.

## Popular o banco local

```powershell
.\scripts\popular-base.ps1
```

O script cria 120 chamados e 24 clientes fictícios, com setores, tipos, técnicos, datas e considerações técnicas. Usa explicitamente o MongoDB local, preserva registros existentes e pode ser repetido sem duplicar a carga.

Para adicionar somente clientes:

```powershell
.\scripts\popular-base.ps1 -SomenteClientes
```

Os dados ficam no MongoDB e não acompanham o Git. Execute a carga na máquina utilizada para a apresentação.

## Usar o painel

Escolher filtros e clicar **Aplicar filtros**. **Limpar filtros** retorna à consulta geral. **Atualizar dados** mantém os filtros aplicados. Expandir **Considerações técnicas** para consultar observações de um chamado.

“Atendido” corresponde a chamado atualmente concluído. O período filtra a abertura; gráfico e ranking também exigem conclusão dentro do período. Datas são apresentadas no fuso de São Paulo. Sem resultados, a tela apresenta valores zero e mensagens informativas.

## Validar e gerar evidências

Com MongoDB local ativo:

```powershell
.\scripts\validar-entrega.ps1
```

O script executa os testes Java e JavaScript, usa um banco temporário separado e salva relatórios em `docs/evidencias/`. Também é possível executar individualmente:

```powershell
mvn "-Dtestes.mongo=true" test
node --test src/test/js/dashboard.test.cjs
```

Sem `-Dtestes.mongo=true`, os testes de integração ficam desativados. A tela não utiliza bibliotecas externas de gráficos: usa SVG e CSS locais.

## Entrega no GitHub

Versionar fontes, scripts, README, documentação e evidências selecionadas. Não adicionar novos arquivos de `target/` ou `.idea/`. A pasta `target/` já possuía arquivos rastreados; adicionar `.gitignore` não remove esse histórico. Veja [as instruções de organização](docs/versionamento.md).

O vídeo ainda precisa ser gravado e disponibilizado, e o link deve ser registrado em `docs/apresentacao.md`. O roteiro não substitui o vídeo.
