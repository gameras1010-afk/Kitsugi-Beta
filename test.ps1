$shell = New-Object -ComObject Shell.Application
$computer = $shell.Namespace(17) # 17 = My Computer
$computer.Items() | Select-Object Name, Path
