#!/bin/bash

usage() {
cat <<EOF
Usage: `basename $0` [OPTIONS]

Initializes files for unit testing.

Options:
    -h         print this help and quit
    -c         clean files
EOF
}

function clean() {
	echo "Cleaning..."
	rm -f I.gb.gz II.gb.gz elegans-I.gz elegans-II.gz proteins.gz env.conf
	rm -Rf dssp
}

while getopts ":ch" opt; do
	case $opt in
		h) 
			usage
			exit 0
			;;
		c)
			clean
			exit 0
			;;
		\?) 
			echo "Invalid option: -$OPTARG" >&2
			echo "Type '`basename $0` -h' to get help."
			exit 1
			;;
	esac
done

# Root directory of the installation
ROOT=../..

####################
# DNA
####################

DL_SCRIPT=$ROOT/tools/download-gb.sh

if [[ ! -f I.gb.gz ]]; then
	echo "Downloading C. elegans chromosome I ..."
	. $DL_SCRIPT I:453231596
fi
if [[ ! -f II.gb.gz ]]; then
	echo "Downloading C. elegans chromosome II ..."
	. $DL_SCRIPT II:453231901
fi

####################
# Proteins
####################

PROTEIN_IDS="966c
9aat
9abp
9ame
9ant
9api
9atc
9ca2
9cgt
9est
9gaa
9gac
9gaf
9gpb
9gss
9hvp
9ica
9icb
9icc
9icd
9ice
9icf
9icg
9ich
9ici
9icj
9ick
9icl
9icm
9icn
9ico
9icp
9icq
9icr
9ics
9ict
9icu
9icv
9icw
9icx
9icy
9ilb
9ins
9jdw
9ldb
9ldt
9lpr
9lyz
9mht
9msi
9nse
9pai
9pap
9pcy
9pti
9rat
9rnt
9rsa
9rub
9wga
9xia
9xim"

DSSP_DIR=dssp

if [[ ! -d $DSSP_DIR ]]; then
	echo "Downloading proteins..."

	mkdir $DSSP_DIR
	for id in $PROTEIN_IDS; do
		wget -O $DSSP_DIR/$id.dssp "ftp://ftp.cmbi.ru.nl/pub/molbio/data/dssp/$id.dssp"
	done
fi

####################
# Convert datasets
####################

LIBS=$ROOT/lib/bytecode-1.8.1.jar:$ROOT/lib/core-1.8.1.jar

if [[ ! -f elegans-I.gz || ! -f elegans-II.gz || ! -f proteins.gz ]]; then
	echo "Creating datasets..."
	java -cp ../bin:$LIBS ua.kiev.icyb.bio.test.Init
fi

####################
# Configuration
####################

if [[ ! -f env.conf ]]; then
	echo "Writing configuration..."

	(
cat <<EOF
elegans-I=elegans-I.gz
elegans-II=elegans-II.gz
prot=proteins.gz
missing=missing.gz
EOF
	) > env.conf
fi

echo "OK, everything is ready for testing."

