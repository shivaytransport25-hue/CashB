$bytes = [System.IO.File]::ReadAllBytes('E:\CashBookRelease\app\CashBookPro.dll')
$str = [System.Text.Encoding]::Unicode.GetString($bytes)
[regex]::Matches($str, '[\x20-\x7E]{3,}') | ForEach-Object { $_.Value } | Select-Object -Unique | Out-File 'g:\My Drive\project\Cash Book\.agents\teamwork\explorer_survey_1\dll_unicode_strings.txt' -Encoding utf8
