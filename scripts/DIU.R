# Differential Isoform Usage Analysis R script
#
# Differential Isoform Usage analysis will be performed by using two different methods: 
#   edgeR spliceVariants function and
#   DEXSeq package

minorFoldfilterTappas <- function(data, gen, minorfilter, minorMethod=c("PROP","FOLD")){
  print ("Removing low expressed minor isoforms")
  moreOne <- names(which(table(gen) > 1))
  iso.sel <- NULL
  iso.sel.prop <- NULL
  
  gene.sel <- NULL
  if(minorMethod=="FOLD"){
    for ( i in moreOne) {
      which(gen==i)
      gene.data <- data[which(gen==i),]
      isoSUM <- apply(gene.data, 1, sum)
      major <- names(which(isoSUM == max(isoSUM)))[1]
      minors <- names(which(isoSUM != max(isoSUM)))
      div <- as.numeric(matrix(rep(gene.data[major,], length(minors)), ncol = ncol(data), length(minors), byrow = T)) / as.matrix(gene.data[minors,])
      is <- names(which(apply(div, 1, min, na.rm = T) < minorfilter))
      iso.sel <- c(iso.sel, major, is)
  
    }
  }else{
    for ( i in moreOne) {
    which(gen==i)
    gene.data <- data[which(gen==i),]

    # by proportion
    geneSUM <- apply(gene.data, 2, sum)
    proportion = t(t(gene.data)/geneSUM)
    is.prop = rownames(proportion[apply(proportion, 1, function(x) any(x>minorfilter)),,drop=F])
    iso.sel <- c(iso.sel, is.prop) 
    
  }}
  
  print(length(iso.sel))
  print ("Done")
  return(iso.sel)
}

### EdgeR spliceVariants

spliceVariant.DS <- function(raw.counts, feature_association, factors) {
  library(edgeR)
  tmm_factors = calcNormFactors(raw.counts)
  DGEList_object_counts = DGEList(counts = round(raw.counts),
                                  lib.size = colSums(raw.counts),
                                  group = as.factor(factors[,1]), genes = NULL,
                                  norm.factors = tmm_factors, 
                                  remove.zeros = FALSE)  
  results = spliceVariants(DGEList_object_counts, geneID=as.character(feature_association[rownames(raw.counts),"id"]), 
                           dispersion=NULL, estimate.genewise.disp = TRUE)$table[,"PValue",drop=F]
  isoPerGene = table(feature_association["id"])
  genesMoreOneIso = isoPerGene[isoPerGene>1] 
  results_multi = results[names(genesMoreOneIso),,drop=F]
  results_multi$adj_PVALUE = p.adjust(results_multi[,1], method = "BH")
  colnames(results_multi) = c("p.value", "q.value")
  return(results_multi)
}

### DEXseq

DEXSeq.DS <- function(raw.counts, feature_association, factors) {
  library(DEXSeq)
  sample_table <- data.frame(row.names = colnames(raw.counts),
                             condition = as.factor(factors[,1]))
  dxd <- DEXSeqDataSet(round(raw.counts), sampleData = sample_table, design = ~ sample + exon + condition:exon,
                       featureID = rownames(raw.counts), 
                       groupID = as.character(feature_association[rownames(raw.counts),"id"]))
  tmm_librarySize_factors = tmm_factors_function(raw.counts, factors)
  sizeFactors(dxd) = c(tmm_librarySize_factors, tmm_librarySize_factors)
  ## pass number of workers? - 2 minimum: , BPPARAM = MulticoreParam(workers = 4))
  #dxd <- estimateDispersions(dxd, BPPARAM = MulticoreParam(workers = 4))
  if (.Platform$OS.type == "unix") {
    bp_param <- MulticoreParam(workers=4);
  } else if (.Platform$OS.type == "windows") {
    bp_param <- SnowParam(workers=4);
  }
  dxd <- estimateDispersions(dxd, BPPARAM = bp_param);
  ## Run the test and get results for exon bins 
  dxd <- testForDEU(dxd)
  ## Summarizing results on gene level
  res <- DEXSeqResults(dxd)
  print(dim(res))
  print(head(res))
  pgq <- perGeneQValue(res, p = "pvalue")
  results <- data.frame(row.names = names(pgq), q.value = pgq) 
  return(results)
}


#### Calculation of TMM normalization factors 

tmm_factors_function <- function(counts, myfactors) {
  library(edgeR)
  myedgeR = DGEList(counts = counts, group = myfactors[,1])
  myedgeR = calcNormFactors(myedgeR, method = "TMM", refColumn = 1)  
  norm_factors_edgeR =  myedgeR$samples$norm.factors
  total = colSums(as.matrix(counts))
  norm_factors_edgeR = norm_factors_edgeR * (total/mean(total))
  return(norm_factors_edgeR)
}

#### Shift in the position of isoforms by level of expression

podiumChange <- function(norm.counts, feature_association, factors) {
  factors = split(myfactors, myfactors[,1])
  norm.counts_mean = data.frame(cond1=apply(norm.counts[,rownames(factors[[1]])], 1, function(x) mean(x)), 
                                cond2=apply(norm.counts[,rownames(factors[[2]])], 1, function(x) mean(x)), 
                                gene=feature_association[rownames(norm.counts),"id"])
  changeOfPosition = as.list(by(norm.counts_mean[,c(1,2)], norm.counts_mean[,"gene"], function(x) {
    prop = t(t(x)/colSums(x))*100
    prop[is.nan(prop)] <- 0
    prop = as.data.frame(prop)
    position = data.frame(cond1_pos = order(prop$cond1, decreasing = TRUE), cond2_pos = order(prop$cond2, decreasing = TRUE), delta=abs(prop$cond1-prop$cond2))
    if (position[1,"cond1_pos"] != position[1,"cond2_pos"]) { r = TRUE }
    else {r = FALSE}
    return (list(podiumChange = r, totalChange = sum(position$delta)/2))
  }))
  A = unlist(sapply(changeOfPosition, function(x) x$podiumChange))
  B = unlist(sapply(changeOfPosition, function(x) x$totalChange))
  return(list(podiumChange=A, totalChange=B))
}

#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential Isoform Usage Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 6) {
  cat("\n")
  stop("Invalid number of arguments.")
}
dataType = ""
method = ""
indir = ""
outdir = ""
filterFC = ""
filteringType = ""
for(i in 1:6) {
    if(length(grep("^-d", args[i])) > 0)
      dataType = substring(args[i], 3)
    else if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-f", args[i])) > 0)
      filterFC = substring(args[i], 3)
    else if(length(grep("^-t", args[i])) > 0)
      filteringType = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(dataType) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(method) == 0 || nchar(filterFC) == 0 || nchar(filteringType) == 0) {
  cat("\n")
  stop("Missing command line argument.")
}

#### determine what type of data to run DEA for

mff <- NULL
if(filterFC != "0") {
    mff = as.numeric(filterFC)
}

deTrans = FALSE
deProteins = FALSE
if(dataType == "trans") {
    deTrans = TRUE
}
if(dataType == "protein") {
    deProteins = TRUE
}

#### Read input data and run analysis

# read expression factors definition table
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "exp_factors.txt"), row.names=1, sep="\t", header=TRUE)
transMatrix <- NULL
transMatrixRaw <- NULL
genes <- NULL

#### check if DIU for transcripts requested
if(deTrans) {
  # read transcript expression matrix normalized
  cat("\nReading normalized transcript matrix file data...")
  transMatrix = read.table(file.path(indir, "transcript_matrix.tsv"), row.names=1, sep="\t", header=TRUE)
  cat("\nRead ", nrow(transMatrix), " normalized transcripts expression data rows")

  # read transcript expression matrix in raw counts
  cat("\nReading raw counts transcript matrix file data...")
  transMatrixRaw = read.table(file.path(indir, "transcript_matrix_raw.tsv"), row.names=1, sep="\t", header=TRUE)
  cat("\nRead ", nrow(transMatrixRaw), " raw counts transcripts expression data rows")

  # read gene transcripts map - this file also contains the transcript lengths, (geneName/transcript/length),
  cat("\nReading gene transcripts map...")
  geneTrans <- read.table(file.path(indir, "gene_transcripts.tsv"), sep="\t", header=TRUE)
  genes <- data.frame("trans"=geneTrans$transcript, "id" = as.character(geneTrans$geneName))
  rownames(genes) <- genes[,1]
  genes[,1] <- NULL

    ### filter matrix
    cat("\nRead ", nrow(transMatrixRaw), " expression data rows")
    
    #Filter transMatrixRaw - need genes
    infoGenes = read.table(file.path(indir, "result_gene_trans.tsv"), sep="\t", quote=NULL, header=FALSE,  stringsAsFactors=FALSE)
    genes_trans = c()
    index = c()
    
    transMatrixRaw = transMatrixRaw[order(rownames(transMatrixRaw)),]
    cat("\nIntersecting DEA information with transcript matrix...")
    for(i in (rownames(transMatrixRaw))){
      if(i %in% infoGenes[,2]){
        genes_trans = c(genes_trans, infoGenes[which(infoGenes[,2]==i),1])
        index = c(index, which(rownames(transMatrixRaw)==i))
      }
    }
    
    transMatrixRaw = transMatrixRaw[index,]
    if(!is.null(mff)){
      cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
      trans = minorFoldfilterTappas(transMatrixRaw, genes_trans, mff, minorMethod=filteringType)
      transMatrixRaw=transMatrixRaw[trans,]
    }
    
    ### end filter

}

#### check if DIU for proteins requested
if(deProteins) {
  # read protein expression matrix normalized
  cat("\nReading normalized protein matrix file data...")
  transMatrix = read.table(file.path(indir, "gene_protein_matrix.tsv"), row.names=1, sep="\t", header=TRUE)
  cat("\nRead ", nrow(transMatrix), " normalized protein expression data rows")

  # read protein expression matrix in raw counts
  cat("\nReading raw counts protein matrix file data...")
  transMatrixRaw = read.table(file.path(indir, "gene_protein_matrix_raw.tsv"), row.names=1, sep="\t", header=TRUE)
  cat("\nRead ", nrow(transMatrixRaw), " raw counts proteins expression data rows")

  # read gene protein map - this file also contains the protein lengths, (geneName/protein/length),
  cat("\nReading gene protein map...")
  geneTrans <- read.table(file.path(indir, "gene_proteins.tsv"), sep="\t", header=TRUE)
  genes <- data.frame("trans"=geneTrans$protein, "id" = as.character(geneTrans$geneName))
  rownames(genes) <- genes[,1]
  genes[,1] <- NULL

    ### filter matrix
    cat("\nRead ", nrow(transMatrixRaw), " expression data rows")
    
    #Filter transMatrixRaw - need genes
    #infoGenes = read.table(file.path(indir, "gene_proteins.tsv"), sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
    infoGenes = geneTrans[,c(1,2)]
    genes_trans = c()
    index = c()
    
    transMatrixRaw = transMatrixRaw[order(rownames(transMatrixRaw)),]
    cat("\nIntersecting DEA information with transcript matrix...")
    for(i in (rownames(transMatrixRaw))){
      if(i %in% infoGenes[,2]){
          genes_trans = c(genes_trans, infoGenes[which(infoGenes[,2]==i),1])
          index = c(index, which(rownames(transMatrixRaw)==i))
      }
    }
    transMatrixRaw = transMatrixRaw[index,]
    if(!is.null(mff)){
      cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
      trans = minorFoldfilterTappas(transMatrixRaw, genes_trans, mff, minorMethod=filteringType)
      transMatrixRaw=transMatrixRaw[trans,]
    }
    
    ### end filter

}

# run DIU Analysis based on selected method
result <- NULL
if(method == "EDGER") {
    cat("\nUsing EdgeR\n")
    result = spliceVariant.DS(transMatrixRaw, genes, myfactors)
} else {
    # not NAs in transMatrixRaw
    nas <- which(!rownames(transMatrixRaw) %in% rownames(genes))
    if(length(nas)>0)
      transMatrixRaw <- transMatrixRaw[-nas,]

    cat("\nUsing DEXSeq\n")
    result = DEXSeq.DS(transMatrixRaw, genes, myfactors)
}

# get the podium change information
pcList = podiumChange(transMatrix, genes, myfactors)

# need to merge the two sets of data into a single dataframe
pcdf <- as.data.frame(pcList$podiumChange)
# divide totalChange / 2
pcdf["totalChange"] <- pcList$totalChange
result_diu <- merge(result, pcdf, by=0)
if(method == "EDGER") {
    colnames(result_diu) <- c("gene", "pValue", "qValue", "podiumChange", "totalChange")
} else {
    colnames(result_diu) <- c("gene", "qValue", "podiumChange", "totalChange")
}

# write results file
write.table(result_diu, file.path(outdir, paste("result_", dataType, ".tsv", sep="")), quote=FALSE, row.names=FALSE, sep="\t")

# write completion file
cat("\nWriting DIUnalysis completed file...")
filedone <- file(file.path(outdir, paste("done_", dataType, ".txt", sep="")))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
