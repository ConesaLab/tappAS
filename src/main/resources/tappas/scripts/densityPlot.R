# Generate density plot given expression levels file
#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input matrix: full path to matrix file
#   data type: TRANS, PROTEIN, GENE
#   output plot: full path to output plot image file
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by H. del Risco

library("ggplot2")

# handle command line arguments - don't want to use external package so users don't have to install it
args = commandArgs(TRUE)
cat("Expression Levels Density Plot arguments: ", args, "\n")
arglen = length(args)
if(arglen != 3)
  stop("Invalid number of arguments.")
infile = ""
outfile = ""
datatype = ""
for(i in 1:3) {
    if(length(grep("^-i", args[i])) > 0)
      infile = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outfile = substring(args[i], 3)
    else if(length(grep("^-d", args[i])) > 0)
      datatype = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(outfile) == 0 || nchar(infile) == 0 || nchar(datatype) == 0)
  stop("Missing command line argument.")

# read data file
inpMatrix = read.table(infile, sep="\t", header=TRUE)
ggm <- stack(inpMatrix)
title <- paste(datatype, " Expresion Levels")
if(ncol(inpMatrix) <= 8) {
    ggp <- ggplot(ggm, aes(x=values)) + ggtitle(title) + xlab("Log10 of mean values") + theme(plot.background = element_blank(), panel.border=element_blank(), legend.background=element_blank(), legend.title=element_blank()) + geom_density(aes(group=ind, colour=ind, fill=ind), alpha=0.3)
} else {
    ggp <- ggplot(ggm, aes(x=values)) + ggtitle(title) + xlab("Log10 of mean values") + theme(plot.background = element_blank(), panel.border=element_blank(), legend.key.size=unit(0.5,"cm"), legend.text=element_text(size=7), legend.background=element_blank(), legend.title=element_blank()) + geom_density(aes(group=ind, colour=ind, fill=ind), alpha=0.3) + guides(col = guide_legend(ncol=2))
}
ggsave(outfile, plot=ggp, device="png", bg="transparent", width=4, height=3, dpi=300)
cat("\nAll done.\n")
