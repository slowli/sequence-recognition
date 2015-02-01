#!/bin/bash

function usage() {
cat <<EOF
Usage: `basename $0` [OPTIONS] [NAME]:[ID] ... [NAME]:[ID]

Downloads chromosome data in Genbank format from NCBI database.
[ID] should be a valid sequence ID in the NCBI database.
[NAME] determines the name of the file output for the sequence; it
is compressed with gzip and named [NAME].gb.gz.

Options:
    -h         print this help and quit
    -d         set the output directory (default is .)
EOF
}

T_DOWNLOAD="http://www.ncbi.nlm.nih.gov/sviewer/viewer.cgi\
	?tool=portal\
	&sendto=on\
	&log$=seqview\
	&db=nuccore\
	&dopt=gbwithparts\
	&sort=\
	&val=SEQID\
	&from=begin\
	&to=end\
	&extrafeat=976"

T_DOWNLOAD=`echo $T_DOWNLOAD | tr -d [:space:]`

function download_seq() {
	local seqname="$1"
	local seqid="$2"
	local link="${T_DOWNLOAD/SEQID/$seqid}"
	local output="$outdir/$seqname.gb"

	echo "Sequence ID: $seqid"
	echo "Output file: $output"
	echo "Downloading sequence from NCBI GenBank database..."
	wget -O "$output" "$link"
	echo "Compressing sequence..."
	gzip -8 "$output"
}

outdir=.

while getopts ":d:h" opt; do
	case $opt in
		d) 
			outdir=$OPTARG
			;;
		h) 
			usage
			exit 0
			;;
		\?) 
			echo "Invalid option: -$OPTARG" >&2
			echo "Type '`basename $0` -h' to get help."
			exit 1
			;;
	esac
done

shift $((OPTIND-1))

if [[ ! -d $outdir ]]; then
	echo "Output directory '$outdir' does not exist!"
	exit 1
fi

params=("$@")
for seq in "${params[@]}"; do
	download_seq `echo $seq | tr ":" " "`
done

