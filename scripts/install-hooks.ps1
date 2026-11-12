$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
git config --local core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar hooks' }
Write-Output 'Hooks pre-commit y pre-push activos. Cada clon debe ejecutar este script.'
