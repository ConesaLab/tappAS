# Expression Matrix Normalization only - no filtering
#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input matrix: full path to raw counts matrix file
#   input factors: full path to input factors file
#   transcripts lengths: full path to transcripts length file
#   output matrix: full path to normalized output matrix
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by H. del Risco

library("MASS")
library("NOISeq")

# Function arguments:
# data:              expression matrix
# length:            transcripts length vector
# factors:           dataframe containing the experimental conditions and samples
# NOTE:              lc is set to 0 until further notice from Lorena!!!

tappas_normalize <- function(data, length, factors) {
  cat("\nProcessing ", nrow(data), " transcript expression data rows")
  factors[,1] = as.factor(factors[,1])
  data_object = readData(data = data, length = length, factors = factors)
  nlengths = as.vector(as.matrix(data_object@featureData@data))
  dataNormalized = tmm(assayData(data_object)$exprs, long = nlengths, refColumn = 1, logratioTrim = 0.3, sumTrim = 0.05, k = 0, lc = 0)
  cat("\nReturning ", nrow(dataNormalized), " transcript expression data rows after normalization.")
  return(dataNormalized)
}

# handle command line arguments - don't want to use external package so users don't have to install it
args = commandArgs(TRUE)
cat("Expression Matrix Normalization script arguments: ", args, "\n")
srcfile = args[1]
expfile = args[2]
translenfile = args[3]
dstfile = args[4]

# read data files
cat("\nReading input matrix file data...")
inputMatrix=read.table(srcfile, row.names=1, sep="\t", header=TRUE)
cat("\nRead ", nrow(inputMatrix), " transcripts expression data rows")
cat("\nReading factors file data...")
myfactors=read.table(expfile, row.names=1, sep="\t", header=TRUE)
cat("\nReading transcript length file data...")
mylengths=read.table(translenfile, row.names=1, sep="\t", header=FALSE)
mylengths=as.vector(mylengths)

# process transcripts
cat("\nNormalizing input matrix...\n")
results = tappas_normalize(data=inputMatrix, length=mylengths, factors=myfactors)
cat("\nSaving results to file...")
for(row in 1:nrow(results)) {
    for(col in 1:ncol(results))
        results[row, col] <- round(results[row, col], 2)
}
write.table(results, dstfile, sep="\t", row.names=TRUE, quote=FALSE)
cat("\nAll done.\n")
