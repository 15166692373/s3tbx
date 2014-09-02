#! /bin/sh

export SNAP_HOME=${installer:sys.installationDir}

if [ -z "$SNAP_HOME" ]; then
    echo
    echo Error: SNAP_HOME not found in your environment.
    echo Please set the SNAP_HOME variable in your environment to match the
    echo location of the SNAP installation
    echo
    exit 2
fi

export PATH=$PATH:$SNAP_HOME/bin

echo ""
echo "Welcome to the SNAP command-line interface!"
echo "The following command-line tools are available:"
echo "  gpt.sh                - General Graph Processing Tool"
echo "  snap-d.sh             - SNAP application launcher for debugging"
echo "  smosGPToNetCDF.sh     - Convert SMOS grid points to NetCDF"
echo "  export-grid-points.sh - Export SMOS grid points to EE or CSV format"
echo "Typing the name of the tool will output its usage information."
echo ""
