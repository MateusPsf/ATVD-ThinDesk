# Resultado da validação

Execução: 2026-11-14 01:03:30 -03:00 (São Paulo).

Commit de referência: 78f52eb. Os testes incluem também os arquivos locais ainda sem commit.

Ambiente: java version "23.0.2" 2025-01-21; Node.js v24.14.0; MongoDB local em 127.0.0.1:27017.

Código de saída Java: 0. Código de saída JavaScript: 0.

## Resultado Java

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.595 s -- in com.pfc.thindesk.dashboard.DashboardErrosTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.154 s -- in com.pfc.thindesk.dashboard.DashboardFiltroTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.915 s -- in com.pfc.thindesk.dashboard.DashboardIntegracaoTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.205 s -- in com.pfc.thindesk.service.ChamadoServiceTest
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Evidências

- [Log Java completo](java.log)
- [Log JavaScript completo](javascript.log)

Os arquivos com.pfc.*.txt apresentam resultados por classe de teste.

Os testes negativos simulam erros 500 e 503: suas exceções nos logs são intencionais e devem ser avaliadas junto ao resumo final.

A integração cria e remove somente um banco temporário de nome aleatório. Os dados do banco thindesk não são modificados.
