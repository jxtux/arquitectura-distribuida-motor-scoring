$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root
docker compose --env-file .env --env-file secrets/bootstrap.env down --remove-orphans
