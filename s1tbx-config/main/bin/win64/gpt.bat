@echo off

set S1TBX_HOME=${installer:sys.installationDir}

"%S1TBX_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate ^
    -Dceres.context=s1tbx ^
    "-Ds1tbx.mainClass=org.esa.beam.framework.gpf.main.GPT" ^
    "-Ds1tbx.home=%S1TBX_HOME%" ^
    "-Ds1tbx.debug=false" ^
	"-Ds1tbx.consoleLog=false" ^
	"-Ds1tbx.logLevel=WARNING" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%S1TBX_HOME%\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%S1TBX_HOME%\jhdf5.dll" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" %*

exit /B 0
