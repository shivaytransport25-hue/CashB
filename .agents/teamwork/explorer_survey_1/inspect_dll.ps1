$bytes = [System.IO.File]::ReadAllBytes('E:\CashBookRelease\app\CashBookPro.dll')
$str = [System.Text.Encoding]::ASCII.GetString($bytes)
[regex]::Matches($str, '[\x20-\x7E]{4,}') | ForEach-Object { $_.Value } | Where-Object { $_ -match 'http|www|file|index|WebView|Navigate|Initialize|Scheme|CoreWebView|Host' } | Select-Object -Unique | Out-File -FilePath 'g:\My Drive\project\Cash Book\.agents\teamwork\explorer_survey_1\dll_strings.txt' -Encoding utf8
