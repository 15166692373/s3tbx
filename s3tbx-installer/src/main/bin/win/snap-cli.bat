@echo off

set SNAP_HOME=${installer:sys.installationDir}

echo.
@echo Welcome to the SNAP command-line interface!
@echo The following command-line tools are available:
@echo   gpt.bat                - General Graph Processing Tool
@echo   snap-d.bat             - SNAP application launcher for debugging
@echo   smosGPToNetCDF.bat     - Convert SMOS grid points to NetCDF
@echo   export-grid-points.bat - Export SMOS grid points to EE or CSV format
@echo Typing the name of the tool will output its usage information.
echo.

cd "%SNAP_HOME%\bin"

prompt $G$S
