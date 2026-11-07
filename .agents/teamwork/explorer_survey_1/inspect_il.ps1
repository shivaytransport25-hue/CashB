# Script to inspect Form1 and InitWebView
Add-Type -AssemblyName System.Reflection
Add-Type -AssemblyName System.Reflection.Metadata
Add-Type -AssemblyName System.Reflection.PortableExecutable

$stream = [System.IO.File]::OpenRead('E:\CashBookRelease\app\CashBookPro.dll')
$peReader = [System.Reflection.PortableExecutable.PEReader]::new($stream)
$metaReader = $peReader.GetMetadataReader()

$userStrings = @()
$handle = [System.Reflection.Metadata.MetadataTokens]::UserStringHandle(1)
for ($i = 1; $i -lt 1000; $i++) {
    try {
        $h = [System.Reflection.Metadata.MetadataTokens]::UserStringHandle($i)
        $str = $metaReader.GetUserString($h)
        if ($str) { $userStrings += "$i : $str" }
    } catch {}
}
$userStrings | Out-File 'g:\My Drive\project\Cash Book\.agents\teamwork\explorer_survey_1\user_strings.txt' -Encoding utf8
$stream.Close()
