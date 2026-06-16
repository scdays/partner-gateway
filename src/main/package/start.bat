@echo off
if not exist META-INF/MANIFEST.MF (jar -xf lib/{0}.jar META-INF/MANIFEST.MF)
for /f "tokens=2,* delims= " %%i in ('type META-INF\MANIFEST.MF ^| findstr JVM') do set jvm=%%i %%j
java -jar %jvm% lib/{0}.jar