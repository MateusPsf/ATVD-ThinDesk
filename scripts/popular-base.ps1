param([switch]$SomenteClientes)

$ErrorActionPreference = 'Stop'
$pastaProjeto = Split-Path -Parent $PSScriptRoot
$maven = Get-Command mvn -ErrorAction SilentlyContinue
$executavelMaven = if ($maven) { $maven.Source } else {
    Get-ChildItem -LiteralPath "$env:USERPROFILE/.m2/wrapper/dists" -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
        Where-Object FullName -Like '*apache-maven-3.9.9*' | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $executavelMaven) { throw 'Instale o Maven ou disponibilize mvn no terminal antes de executar esta carga.' }
Push-Location $pastaProjeto
try {
    New-Item -ItemType Directory -Path target -Force | Out-Null
    $arquivoClasspath = Join-Path $pastaProjeto 'target/classpath-popular-base.txt'
    & $executavelMaven dependency:build-classpath "-Dmdep.outputFile=$arquivoClasspath" -q
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar as dependências da carga.' }
    $classpathCarga = (Get-Content -LiteralPath $arquivoClasspath -Raw).Trim()
    if ($SomenteClientes) {
        & java --class-path $classpathCarga (Join-Path $PSScriptRoot 'PopularBase.java') '--somente-clientes'
    } else {
        & java --class-path $classpathCarga (Join-Path $PSScriptRoot 'PopularBase.java')
    }
    if ($LASTEXITCODE -ne 0) { throw 'A carga não foi concluída. Confira se o MongoDB local está ativo.' }
} finally {
    Pop-Location
}
