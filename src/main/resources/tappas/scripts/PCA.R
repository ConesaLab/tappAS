#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# PCA script arguments:
#   matrix  filepath: (full directory path)
#   output  filepath: (full directory path)
#
# Note:   All arguments are required. Expression matrix must be already normalized.
#
# Written by H. del Risco

library("MASS")
library("NOISeq")

# handle command line arguments
args = commandArgs(TRUE)
cat("PCA script arguments: ", args, "\n")
matrixfile = args[1]
factorsfile = args[2]
outfile = args[3]

# read data files
cat("\nReading expression matrix file data...")
expMatrix=read.table(matrixfile, row.names=1, sep="\t", header=TRUE)
cat("\nRead ", nrow(expMatrix), " transcripts expression data rows")
cat("\nReading factors file data...")
expFactors=read.table(factorsfile, row.names=1, sep="\t", header=TRUE)

# process transcripts
cat("\nPerforming PCA...\n")
indata = readData(data = expMatrix, factors = expFactors)
pcadata = dat(indata, type = "PCA", norm = TRUE)
cat("\nSaving results to file...")
cat("#VAR\n", file=outfile)
write.table(pcadata@dat$result$var.exp, outfile, sep="\t", append = TRUE, row.names=FALSE, col.names = FALSE, quote=FALSE)
cat("#SCORES\n", file=outfile, append=TRUE)
write.table(pcadata@dat$result$scores, outfile, sep="\t", append = TRUE, row.names=FALSE, col.names = FALSE, quote=FALSE)
cat("\nAll done.\n")
