param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!$Test) {
    $port = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect('127.0.0.1', $port)
    } catch [System.Net.Sockets.SocketException] {
        if ($_.Exception.SocketErrorCode -ne 'ConnectionRefused') { throw }
    } finally {
        $occupied = $client.Connected
        $client.Dispose()
    }

    if ($occupied) {
        $page = $null
        try {
            $page = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$port/" -TimeoutSec 3 -MaximumRedirection 0
        } catch {
            # The listener may not be an HTTP server.
        }
        if ($page -and $page.Content.Contains('href="desktop-client/index.html"') -and
                $page.Content.Contains('href="mobile-client/index.html"')) {
            Write-Host "La messagerie est deja lancee. Ouvrez http://localhost:$port/"
            exit 0
        }
        throw "Le port $port est occupe. Impossible de confirmer que la messagerie y fonctionne."
    }
}

$mysql = Join-Path $env:ProgramFiles 'MySQL\MySQL Server 8.0\bin\mysql.exe'
$state = Join-Path $env:LOCALAPPDATA 'EfreiMessagerie'
$secret = Join-Path $state 'mysql-local.xml'
if (!$env:MESSAGERIE_DB_PASSWORD) {
    if (Test-Path -LiteralPath $secret) {
        $credential = Import-Clixml -LiteralPath $secret
    } else {
        $appUser = 'messagerie_app_' + [Guid]::NewGuid().ToString('N').Substring(0,8)
        $password = 'Ag9!' + [Guid]::NewGuid().ToString('N')
        Write-Host 'Premiere installation : entrez le mot de passe root MySQL utilise dans Workbench.'
        $sql = "CREATE DATABASE IF NOT EXISTS messagerie CHARACTER SET utf8mb4; CREATE USER '$appUser'@'localhost' IDENTIFIED BY '$password'; GRANT ALL PRIVILEGES ON messagerie.* TO '$appUser'@'localhost';"
        $sql | & $mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p
        if ($LASTEXITCODE -ne 0) { throw 'Configuration MySQL interrompue.' }
        $credential = New-Object PSCredential($appUser, (ConvertTo-SecureString $password -AsPlainText -Force))
        New-Item -ItemType Directory -Force -Path $state | Out-Null
        $credential | Export-Clixml -LiteralPath $secret
    }
    $env:MESSAGERIE_DB_USER = $credential.UserName
    $env:MESSAGERIE_DB_PASSWORD = $credential.GetNetworkCredential().Password
}
if ($Test) {
    mvn -B -ntp test
} else {
    mvn -B -ntp spring-boot:run
}
exit $LASTEXITCODE
