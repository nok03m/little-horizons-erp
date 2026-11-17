param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $ApplicationArgs = @()
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jarPath = Join-Path $repositoryRoot 'target\little-horizons-erp-1.0.0.jar'

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw 'Build the application first with mvn verify.'
}
if (-not (Test-NetConnection -ComputerName '127.0.0.1' -Port 3306 -InformationLevel Quiet)) {
    throw 'Local MySQL is not reachable at 127.0.0.1:3306.'
}

$credential = Get-Credential -UserName 'app_user' -Message 'Local staging DB (app_db on 127.0.0.1 only)'
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3306'
$env:DB_NAME = 'app_db'
$env:DB_USER = 'app_user'
$env:DB_PASSWORD = $credential.GetNetworkCredential().Password
$env:DB_PARAMS = 'sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8'

try {
    & java -jar $jarPath @ApplicationArgs
    exit $LASTEXITCODE
}
finally {
    Remove-Item Env:\DB_HOST, Env:\DB_PORT, Env:\DB_NAME, Env:\DB_USER, Env:\DB_PASSWORD, Env:\DB_PARAMS -ErrorAction SilentlyContinue
}
