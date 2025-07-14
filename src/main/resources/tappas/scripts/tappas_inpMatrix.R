# Expression Matrix Normalization and Filtering
#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input matrix: full path to raw counts matrix file
#   input factors: full path to input factors file
#   transcripts lengths: full path to transcripts length file
#   output matrix: full path to normalized and filtered output matrix
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by H. del Risco & Pedro Salguero García

library("MASS")
library("NOISeq")

# CPM (method 1): The user chooses a value for the parameter counts per million (CPM) in a sample under
# which a feature is considered to have low counts. The cutoff for a condition with s samples is CPM × s.
# Features with sum of expression values below the condition cutoff in all conditions are removed. Also a
# cutoff for the coefficient of variation (in percentage) per condition may be established to eliminate features
# with inconsistent expression values.

# Function arguments:
# data:              expression matrix
# length:            transcripts length vector
# factors:           dataframe containing the experimental conditions and samples
# NOTES:              lc is set to 0 until further notice from Lorena!!!

tappas_filter <- function(data, length, factors, filterCPM, filterCOV) {
  cat("\nProcessing ", nrow(data), " transcript expression data rows")
  factors[,1] = as.factor(factors[,1])
  dataFiltered = filtered.data(data, factor = factors[,1], norm = FALSE, depth = NULL, method = 1, cv.cutoff = filterCOV, cpm = filterCPM, p.adj = "fdr")
  cat("\nReturning ", nrow(dataFiltered), " transcript expression data rows after filtering.")
  return(dataFiltered)
}

tappas_normalize <- function(data, length, factors) {
  cat("\nProcessing ", nrow(data), " transcript expression data rows")
  factors[,1] = as.factor(factors[,1])
  data_object = readData(data = data, length = length, factors = factors)
  nlengths = as.vector(as.matrix(data_object@featureData@data))
  dataNormalized = tmm(assayData(data_object)$exprs, long = nlengths, refColumn = 1, logratioTrim = 0.3, sumTrim = 0.05, k = 0, lc = 0)
  cat("\nReturning ", nrow(dataNormalized), " transcript expression data rows after normalization.")
  return(dataNormalized)
}

tappas_filterNormalize <- function(data, length, factors, filterCPM, filterCOV) {
  set.seed(123)
  cat("\nProcessing ", nrow(data), " transcript expression data rows")
  factors[,1] = as.factor(factors[,1])
  dataFiltered = filtered.data(data, factor = factors[,1], norm = FALSE, depth = NULL, method = 1, cv.cutoff = filterCOV, cpm = filterCPM, p.adj = "fdr")
  cat("\n", nrow(dataFiltered), " transcript expression data rows left after filtering")
  data_object = readData(data = dataFiltered, length = length, factors = factors)
  nlengths = as.vector(as.matrix(data_object@featureData@data))
  dataNormalized = tmm(assayData(data_object)$exprs, long = nlengths, refColumn = 1, logratioTrim = 0.3, sumTrim = 0.05, k = 0, lc = 0)
  cat("\nReturning ", nrow(dataNormalized), " transcript expression data rows after filtering and normalization.")
  return(dataNormalized)
}

# handle command line arguments - don't want to use external package so users don't have to install it
args = commandArgs(TRUE)
cat("Expression Matrix Filtering and Normalization script arguments: ", args, "\n")
srcfile = args[1]
expfile = args[2]
translenfile = args[3]
dstfile = args[4]
ncpm = as.numeric(args[5])
ncov = as.numeric(args[6])
norm = as.character(args[7])
#args = commandArgs(TRUE)
#cat("Expression Matrix Filtering and Normalization script arguments: ", args, "\n")
#srcfile = "input_matrix.tsv"
#expfile = "exp_factors.txt"
#translenfile = "transcript_lengths.tsv"
#dstfile = "res.tsv"
#ncpm = 1
#ncov = 100
#norm = "Y"

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

#nothing
if(ncpm==0 & ncov==0 & norm=="N"){
    results = inputMatrix
    
    if(nrow(results[-which(apply(results, 1, function(row) any(row < 0))),]) != 0){
        print("tappAS found negative values in your expression matrix. These rows will not appear in the project.")
        results = results[-which(apply(results, 1, function(row) any(row < 0))),]
    }

}
#norm
if(ncpm==0 & ncov==0 & norm=="Y"){

    aux = inputMatrix

    if(nrow(aux[-which(apply(aux, 1, function(row) any(row < 0))),]) != 0){
        print("tappAS found negative values in your expression matrix. These rows will not appear in the project.")
        aux = aux[-which(apply(aux, 1, function(row) any(row < 0))),]
    }

    results = tappas_normalize(data=aux, length=mylengths, factors=myfactors)

    if(nrow(results[-which(apply(results, 1, function(row) any(row < 0))),]) != 0){
        results = results[-which(apply(results, 1, function(row) any(row < 0))),]
    }

}
#filter
if((ncpm!=0 | ncov!=0) & norm=="N"){

    aux = inputMatrix

    if(nrow(aux[-which(apply(aux, 1, function(row) any(row < 0))),]) != 0){
        print("tappAS found negative values in your expression matrix. These rows will not appear in the project.")
        aux = aux[-which(apply(aux, 1, function(row) any(row < 0))),]
    }

    cat("\nFiltering, cutoff: ", ncpm, ", COV: ", ncov, ", and normalizing input matrix...\n")
    results = tappas_filter(data=aux, length=mylengths, factors=myfactors, filterCPM=ncpm, filterCOV=ncov)

    if(nrow(results[-which(apply(results, 1, function(row) any(row < 0))),]) != 0){
        results = results[-which(apply(results, 1, function(row) any(row < 0))),]
    }

}
#norm+filter
if((ncpm!=0 | ncov!=0) & norm=="Y"){

    aux = inputMatrix

    if(nrow(aux[-which(apply(aux, 1, function(row) any(row < 0))),]) != 0){
        print("tappAS found negative values in your expression matrix. These rows will not appear in the project.")
        aux = aux[-which(apply(aux, 1, function(row) any(row < 0))),]
    }

    cat("\nFiltering, cutoff: ", ncpm, ", COV: ", ncov, ", and normalizing input matrix...\n")
    results = tappas_filterNormalize(data=aux, length=mylengths, factors=myfactors, filterCPM=ncpm, filterCOV=ncov)

    if(nrow(results[-which(apply(results, 1, function(row) any(row < 0))),]) != 0){
        results = results[-which(apply(results, 1, function(row) any(row < 0))),]
    }

}

#Saving results
cat("\nSaving results to file...")
for(row in 1:nrow(results)) {
    for(col in 1:ncol(results))
        results[row, col] <- round(results[row, col], 2)
}
write.table(results, dstfile, sep="\t", row.names=TRUE, quote=FALSE)
cat("\nAll done.\n")

