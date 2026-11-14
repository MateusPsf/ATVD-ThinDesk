# Versionamento e entrega no GitHub

## Situação conferida em 14/11/2026

Repositório configurado: [MateusPsf/ATVD-ThinDesk](https://github.com/MateusPsf/ATVD-ThinDesk).

As referências foram consultadas no remoto e atualizadas localmente com fetch, sem criar commits nem enviar alterações:

| Referência | Commit verificado | Situação |
| --- | --- | --- |
| develop local e origin/develop | 78f52eb | Implementação do dashboard e carga de dados |
| origin/main | 5bdfab4 | Merge da develop para main; contém a implementação |
| Implementação da API | fe3a3b2 | Commit preservado no histórico |

O [histórico capturado](evidencias/historico-git.txt) mostra os commits e merges. No momento da conferência, main possui dois commits adicionais em relação à develop, e não há commits exclusivos da develop ausentes na main. Isso corresponde a merges existentes; não significa falta da implementação na main.

## Gitflow

Foi identificado o fluxo develop → pull request/merge → main. Não há, nas referências consultadas, uma branch feature exclusiva para o desenvolvimento original da API. Portanto, há um fluxo simplificado com branches de integração e entrega, e não evidência de todas as etapas de um Gitflow completo.

Não foi criada uma branch retroativa para sugerir um histórico que não ocorreu. Se a avaliação exigir especificamente branches feature/release, registrar essa limitação e seguir esse fluxo nas próximas alterações.

## Registrar a documentação complementar

Os arquivos preparados nesta revisão estão apenas locais. Para seguir um fluxo de feature na finalização, o responsável pode executar, após revisar os arquivos:

```bash
git switch develop
git fetch origin
git merge origin/main
git switch -c feature/documentacao-entrega
git add .gitignore README.md docs/ scripts/validar-entrega.ps1
git commit -m "docs: completa documentacao e evidencias da entrega"
git push -u origin feature/documentacao-entrega
```

Se houver conflitos, resolvê-los antes de continuar. Abrir pull request da feature para develop; após integrar, abrir pull request de develop para main. Não excluir develop após o merge.

Esses comandos são instruções para o responsável: não foram executados automaticamente nesta revisão, exceto a atualização de referências em leitura com fetch.

## Arquivos gerados

Foram identificados 68 arquivos já rastreados em target. O novo .gitignore evita novos arquivos gerados, mas não retira do índice arquivos que já estavam versionados.

Para deixar de versioná-los mantendo os arquivos no computador, o responsável pode revisar uma alteração separada:

```bash
git rm -r --cached -- target/
git status
git commit -m "chore: deixa de versionar arquivos de compilacao"
```

Essa limpeza não foi executada, para não misturar remoções do índice com a entrega de documentação. Não executar git clean para esse objetivo.

## Conferência final no navegador

Após enviar e integrar as alterações, abrir a branch main no GitHub e verificar:

- README visível e links funcionando.
- Documento de requisitos e API acessíveis em docs.
- Relatórios e capturas presentes em docs/evidencias.
- Fontes da API e do dashboard presentes.
- Scripts de carga e de validação presentes.
- Link do vídeo preenchido em docs/apresentacao.md e acessível ao avaliador.

O vídeo ainda não foi gravado; o roteiro é um material de apoio, não o entregável audiovisual.
