param([string]$MavenPath)

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
if (-not $MavenPath) {
    $comandoMaven = Get-Command mvn -ErrorAction SilentlyContinue
    $MavenPath = if ($comandoMaven) { $comandoMaven.Source } else {
        Get-ChildItem -LiteralPath "$env:USERPROFILE/.m2/wrapper/dists" -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
            Where-Object FullName -Like '*apache-maven-3.9.9*' | Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $MavenPath) { throw 'Disponibilize o Maven no terminal ou informe -MavenPath com o caminho de mvn.cmd.' }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'O Node.js é necessário para os testes da tela.' }
$pastaEvidencias = Join-Path $raizProjeto 'docs/evidencias'
$pastaValidacao = Join-Path $raizProjeto ('target/entrega-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $pastaEvidencias,$pastaValidacao -Force | Out-Null
# Uma cópia nova evita misturar classes produzidas pelo Maven e pelo editor.
Copy-Item -LiteralPath (Join-Path $raizProjeto 'pom.xml') -Destination (Join-Path $pastaValidacao 'pom.xml')
Copy-Item -LiteralPath (Join-Path $raizProjeto 'src') -Destination (Join-Path $pastaValidacao 'src') -Recurse
Push-Location $raizProjeto
try {
    $registroJava = Join-Path $pastaEvidencias 'java.log'
    & $MavenPath -B -f (Join-Path $pastaValidacao 'pom.xml') '-Dtestes.mongo=true' test *> $registroJava
    $resultadoJava = $LASTEXITCODE
    $registroJS = Join-Path $pastaEvidencias 'javascript.log'
    & node --test --test-reporter=tap src/test/js/dashboard.test.cjs *> $registroJS
    $resultadoJS = $LASTEXITCODE
    $relatorios = Join-Path $pastaValidacao 'target/surefire-reports'
    Get-ChildItem -Path $relatorios -Filter '*.txt' -ErrorAction SilentlyContinue |
        Copy-Item -Destination $pastaEvidencias -Force
    $resumoTestes = Get-Content -LiteralPath $registroJava | Where-Object { $_ -match 'Tests run:|BUILD SUCCESS|BUILD FAILURE' }
    $versaoJava = (& java -version 2>&1 | Select-Object -First 1).ToString()
    $versaoNode = (& node --version).ToString()
    $commitAtual = (& git rev-parse --short HEAD).ToString()
    $dataExecucao = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow, 'E. South America Standard Time').ToString('yyyy-MM-dd HH:mm:ss zzz')
    $linhas = @(
        '# Resultado da validação', '', "Execução: $dataExecucao (São Paulo).", '',
        "Commit de referência: $commitAtual. Os testes incluem também os arquivos locais ainda sem commit.", '',
        "Ambiente: $versaoJava; Node.js $versaoNode; MongoDB local em 127.0.0.1:27017.", '',
        "Código de saída Java: $resultadoJava. Código de saída JavaScript: $resultadoJS.", '',
        '## Resultado Java', '', '```text'
    ) + $resumoTestes + @(
        '```', '', '## Evidências', '',
        '- [Log Java completo](java.log)', '- [Log JavaScript completo](javascript.log)', '',
        'Os arquivos com.pfc.*.txt apresentam resultados por classe de teste.', '',
        'Os testes negativos simulam erros 500 e 503: suas exceções nos logs são intencionais e devem ser avaliadas junto ao resumo final.', '',
        'A integração cria e remove somente um banco temporário de nome aleatório. Os dados do banco thindesk não são modificados.'
    )
    Set-Content -LiteralPath (Join-Path $pastaEvidencias 'resultado.md') -Value $linhas -Encoding utf8
    $resumoTestes | Write-Output
    if ($resultadoJava -ne 0 -or $resultadoJS -ne 0) { throw 'Há testes com falha. Consulte docs/evidencias antes de entregar.' }
    'Validação concluída. Evidências salvas em docs/evidencias.'
} finally { Pop-Location }
