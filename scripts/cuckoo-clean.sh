#!/bin/sh -e

CUCKOO_INIT="/etc/init.d/cuckoo"
CUCKOO_JAVA_INIT="/etc/init.d/hsn2-cuckoo-java"
CUCKOO_HOME="/opt/hsn2/cuckoo"
CUCKOO_MAIN="cuckoo.py"
CUCKOO_CLEAN_FLAG="--clean"

$CUCKOO_JAVA_INIT stop
$CUCKOO_INIT stop
${CUCKOO_HOME}/${CUCKOO_MAIN} $CUCKOO_CLEAN_FLAG
$CUCKOO_INIT start
$CUCKOO_JAVA_INIT start
