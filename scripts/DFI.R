# Differential Feature Inclusion Analysis R script
#
# Differential Isoform Usage analysis will be performed by using two different methods: 
#   edgeR spliceVariants function and
#   DEXSeq package
#
# Warning: DEXSeq removes the ":" from "GO:" ids so we replace it for the input data
#          with "GO__" and back to "GO:" for the results
#

library(plyr)

round_df <- function(x, digits) {
    # round all numeric variables
    # x: data frame
    # digits: number of digits to round
    numeric_columns <- sapply(x, mode) == 'numeric'
    x[numeric_columns] <-  round(x[numeric_columns], digits)
    x
}

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
  cat("EdgeR start analysis...\n")
  sampleNumber = nrow(myfactors)
  cat("Number of samples is ", sampleNumber, "\n")
  DGEList_object_counts = DGEList(counts = round(raw.counts),
                                  lib.size = rep(100000000, times=sampleNumber),
                                  group = as.factor(factors[,1]), genes = NULL,
                                  norm.factors = rep(1, sampleNumber), 
                                  remove.zeros = FALSE)  
  cat("Calling spliceVariants2...\n")
  results = spliceVariants2(DGEList_object_counts, geneID=as.character(feature_association[rownames(raw.counts),"id"]), 
                           dispersion=NULL, estimate.genewise.disp = TRUE)
  print(head(results$table))
  cat("Extracting PValues...\n")
  p_values = results$table[,"PValue", drop=F]
  #print(p_values)
  non_motif_slope = results$coefficients[,3]+results$coefficients[,4]
  motif_slope = results$coefficients[,3]
  favSecondCondition = as.integer((motif_slope>0 & non_motif_slope<0) | 
                                  (motif_slope>0 & non_motif_slope>0 & abs(motif_slope)>abs(non_motif_slope)) |
                                  (motif_slope<0 & non_motif_slope<0 & abs(motif_slope)<abs(non_motif_slope)))   
  cat("Adjusting PValues...\n")
  adj_PVALUE = p.adjust(p_values[,"PValue"], method = "BH")
  return(data.frame(p_values, adj_PVALUE, favSecondCondition))
}

### DEXseq

DEXSeq.DS <- function(raw.counts, feature_association, factors) {
  library(DEXSeq)
  cat("DEXSeq start analysis...\n")
  sample_table <- data.frame(row.names = colnames(raw.counts),
                             condition = as.factor(factors[,1]))
  cat("DEXSeq sample table generated...\n")
  print(head(sample_table))
  cat("\n")
  dxd <- DEXSeqDataSet(round(raw.counts), sampleData = sample_table, design = ~ sample + exon + condition:exon,
                       featureID = rownames(raw.counts), 
                       groupID = as.character(feature_association[rownames(raw.counts),"id"]))
  cat("DEXSeq dataset generated...\n")
  print(head(dxd))
  cat("\n")
  #tmm_librarySize_factors = tmm_factors_function(raw.counts, factors)
  sampleNumber = nrow(myfactors)
  cat("Number of total samples is ", sampleNumber, "\n")
  sizeFactors(dxd) = rep(1, sampleNumber*2)
  print(sizeFactors(dxd))
  #dxd <- estimateDispersions(dxd)
  if (.Platform$OS.type == "unix") {
    bp_param <- MulticoreParam(workers=4);
  } else if (.Platform$OS.type == "windows") {
    bp_param <- SnowParam(workers=4);
  }
  
  ##CODE to prevent error (if only have one gene (with and without) does not work)
  if((dim(dxd)[1])/2<=1){return(NULL)}
  
  dxd <- tryCatch({estimateDispersions(dxd, BPPARAM = bp_param)}, #try
                  error=function(cond){return(NA)} # error
  )
  
  if(is.na(dxd)){
    cat("It is possible you have a low number of genes/features and the method can not stimate the results.\n")
    return(results) # low samples/genes and we can not calculate the results
  } 
  
  dxd <- estimateDispersions(dxd, BPPARAM = bp_param);
  ## Run the test and get results for exon bins 
  dxd <- testForDEU(dxd)
  ## Summarizing results on gene level
  cat("Get DEXSeqResults\n")
  res <- DEXSeqResults(dxd)
  #print(dim(res))
  #print(head(res))
  
  ##CODE to prevent error
  wTest <- which(!is.na(res$padj))
  pvals = res[["pvalue"]][wTest]
  geneID = factor(res[["groupID"]][wTest])
  geneSplit = split(seq(along = geneID), geneID)
  pGene = sapply(geneSplit, function(i) min(pvals[i]))
  theta = unique(sort(pGene))
  if(length(theta)==1){return(NULL)}
  
  pgq <- perGeneQValue(res, p = "pvalue")
  cat("Get Results\n")
  results <- data.frame(row.names = names(pgq), q.value = pgq) 
  #print(head(results))
  return(results)
}

# edgeR spliceVariant function modified to retrieve glm model coefficients

spliceVariants2 <- function (y, geneID, dispersion = NULL, group = NULL, estimate.genewise.disp = TRUE, trace = FALSE) {
  library(methods)

  if (is(y, "DGEList")) {
    y.mat <- y$counts
    if (is.null(group)) 
      group <- y$samples$group
  }
  else {
    y.mat <- as.matrix(y)
    if (is.null(group)) 
      stop("y is a matrix and no group argument has been supplied. Please supply group argument.")
  }
  
  geneID <- as.vector(unlist(geneID))

  o <- order(geneID)
  geneID <- geneID[o]
  y.mat <- y.mat[o, ]
  uniqIDs <- unique(geneID)
  if (is.null(dispersion)) {
    if (estimate.genewise.disp) {
      dispersion <- estimateExonGenewiseDisp(y.mat, geneID, group)
      genewise.disp <- TRUE
      if (trace) 
        cat("Computing genewise dispersions from exon counts.\n")
    }
    else {
      dispersion <- estimateCommonDisp(DGEList(counts = y.mat, group = group))
      genewise.disp <- FALSE
      if (trace) 
        cat("Computing common dispersion across all exons.\n")
    }
  }
  else {
    if (length(dispersion) == length(uniqIDs)) {
      if (is.null(names(dispersion))) 
        stop("names(dispersion) is NULL. All names of dispersion must be unique geneID.\n")
      matches <- match(uniqIDs, names(dispersion))
      if (any(is.na(matches)) | any(duplicated(matches))) 
        stop("names(dispersion) of dispersion do not have a one-to-one mapping to unique geneID. All names of dispersion must be unique geneID.\n")
      dispersion <- dispersion[matches]
      genewise.disp <- TRUE
    }
    if (length(dispersion) == 1) {
      genewise.disp <- FALSE
    }
  }

  keep <- rowSums(y.mat) > 0
  exons <- y.mat[keep, ]
  rownames(exons) <- geneID[keep]
  uniqIDs <- unique(geneID[keep])
  na.vec <- rep(NA, length(uniqIDs))
  if (genewise.disp) 
    dispersion <- dispersion[names(dispersion) %in% uniqIDs]
  if (!genewise.disp) {
    dispersion <- rep(dispersion, length(uniqIDs))
    names(dispersion) <- uniqIDs
  }
  nexons <- na.vec
  dummy <- rowsum(rep(1, nrow(exons)), rownames(exons))
  nexons <- as.vector(dummy)
  names(nexons) <- rownames(dummy)
  mm <- match(uniqIDs, names(nexons))
  nexons <- nexons[mm]
  if (trace) 
    cat("Max number exons: ", max(nexons), "\n")
  splicevars.out <- data.frame(logFC = na.vec, logCPM = na.vec, 
                               LR = na.vec, PValue = na.vec)
  rownames(splicevars.out) <- uniqIDs
  
  coefficients.out <- data.frame(Intercept = na.vec, exon.this2 = na.vec, 
                                 group.this2 = na.vec, exon.this2_group.this2 = na.vec)
  rownames(coefficients.out) <- uniqIDs
  
  abundance <- na.vec
  for (i.exons in sort(unique(nexons))) {
    this.genes <- nexons == i.exons
    full.index <- rownames(exons) %in% uniqIDs[this.genes]
    if (any(this.genes)) {
      gene.counts.mat <- matrix(t(exons[full.index, ]), 
                                nrow = sum(this.genes), ncol = ncol(exons) * 
                                  i.exons, byrow = TRUE)
      if (i.exons == 1) {
        abundance[this.genes] <- aveLogCPM(gene.counts.mat)
        splicevars.out$LR[this.genes] <- 0
        splicevars.out$PValue[this.genes] <- 1
        coefficients.out$Intercept[this.genes] <- 0
        coefficients.out$exon.this2[this.genes] <- 0
        coefficients.out$group.this2[this.genes] <- 0
        coefficients.out$exon.this2_group.this2[this.genes] <- 0
        
      }
      
      else {
        exon.this <- factor(rep(1:i.exons, each = ncol(exons)))
        group.this <- as.factor(rep(group, i.exons))
        X.full <- model.matrix(~exon.this + group.this + 
                                 exon.this:group.this)
        X.null <- model.matrix(~exon.this + group.this)
        coef <- (ncol(X.null) + 1):ncol(X.full)
        fit.this <- glmFit(gene.counts.mat, X.full, dispersion[this.genes], 
                           offset = 0, prior.count = 0)
        abundance[this.genes] <- aveLogCPM(gene.counts.mat)
        results.this <- glmLRT(fit.this, coef = coef)
        if (sum(this.genes) == 1) {
          splicevars.out$LR[this.genes] <- results.this$table$LR[1]
          splicevars.out$PValue[this.genes] <- results.this$table$PValue[1]
          #coefficients = fit.this$coefficients
          coefficients.out$Intercept[this.genes] <- fit.this$coefficients[,1]
          coefficients.out$exon.this2[this.genes] <- fit.this$coefficients[,2]
          coefficients.out$group.this2[this.genes] <- fit.this$coefficients[,3]
          coefficients.out$exon.this2_group.this2[this.genes] <- fit.this$coefficients[,4]
        }
        else {
          splicevars.out$LR[this.genes] <- results.this$table$LR
          splicevars.out$PValue[this.genes] <- results.this$table$PValue
          coefficients = fit.this$coefficients
          coefficients.out$Intercept[this.genes] <- fit.this$coefficients[,1]
          coefficients.out$exon.this2[this.genes] <- fit.this$coefficients[,2]
          coefficients.out$group.this2[this.genes] <- fit.this$coefficients[,3]
          coefficients.out$exon.this2_group.this2[this.genes] <- fit.this$coefficients[,4]
          
        }
      }
    }
  }

  splicevars.out$logCPM <- abundance
  if (!genewise.disp) 
    dispersion <- dispersion[1]
  
  print(head(coefficients.out))
  print(head(coefficients))
  
  new("DGEExact", list(table = splicevars.out, comparison = NULL, 
                       genes = data.frame(GeneID = uniqIDs), dispersion = dispersion, coefficients = coefficients.out))
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
cat("Differential Feature Inclusion Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 14)
  stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""
filteringType = ""
filterFC = ""
sig = ""
inFileCount = ""
outputTestFeatures = ""
outputTestGenes = ""
analysisId = ""
indirDFI = "";

for(i in 1:14) {
    if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-d", args[i])) > 0)
      indirDFI = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-f", args[i])) > 0)
      filterFC = substring(args[i], 3)
    else if(length(grep("^-t", args[i])) > 0)
      filteringType = substring(args[i], 3)
    else if(length(grep("^-s", args[i])) > 0)
      sig = substring(args[i], 3)
    else if(length(grep("^-c", args[i])) > 0)
      inFileCount = substring(args[i], 3)
    else if(length(grep("^-g1", args[i])) > 0)
      outputTestFeatures = substring(args[i], 4)
    else if(length(grep("^-g2", args[i])) > 0)
      outputTestGenes = substring(args[i], 4)
    else if(length(grep("^-a", args[i])) > 0)
      analysisId = substring(args[i], 3)
    else if(length(grep("^-lt", args[i])) > 0)
      transFeatures = substring(args[i], 4)
    else if(length(grep("^-lp", args[i])) > 0)
      proteinFeatures = substring(args[i], 4)
    else if(length(grep("^-x", args[i])) > 0)
      dfiMatrixTest = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(dfiMatrixTest) == 0 || nchar(transFeatures) == 0 || nchar(proteinFeatures) == 0 || nchar(indirDFI) == 0 || nchar(analysisId) == 0 || nchar(outputTestFeatures) == 0 || nchar(outputTestGenes) == 0 || nchar(inFileCount) == 0 || nchar(sig) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(method) == 0 || nchar(filterFC) == 0 || nchar(filteringType) == 0)
  stop("Missing command line argument.")

#### Read input data and run analysis

mff <- NULL
if(filterFC != "0") {
  mff = as.numeric(filterFC)
}

sig = as.numeric(sig)

# read expression factors definition table
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "exp_factors.txt"), row.names=1, sep="\t", header=TRUE)

# read feature expression matrix normalized
cat("\nReading normalized feature matrix file data...")

featureMatrix = read.table(file.path(indirDFI, paste0("dfi_feature_matrix.",as.character(analysisId),".tsv")), row.names=1, sep="\t", quote=NULL, header=TRUE)
row.names(featureMatrix) <- lapply(row.names(featureMatrix), function(x) {gsub("GO:", "GO__", x)})
cat("\nRead ", nrow(featureMatrix), " normalized feature expression data rows")

# read feature expression matrix in raw counts
cat("\nReading raw counts feature matrix file data...")
featureMatrixRaw = read.table(file.path(indirDFI, paste0("dfi_feature_matrix_raw.",as.character(analysisId),".tsv")), row.names=1, sep="\t", quote=NULL, header=TRUE)
row.names(featureMatrixRaw) <- lapply(row.names(featureMatrixRaw), function(x) {gsub("GO:", "GO__", x)})
cat("\nRead ", nrow(featureMatrixRaw), " raw counts feature expression data rows")

# read gene feature map (gene-feature[-pos]-on/off to gene-feature[-pos]), where the position is optional
cat("\nReading gene features map...")

geneFeatures <- read.table(file.path(indirDFI, paste0("dfi_feature_id_map.",as.character(analysisId),".tsv")), sep="\t", quote=NULL, header=TRUE)
geneFeatures <- data.frame(lapply(geneFeatures, function(x) {gsub("GO:", "GO__", x)}))
genes <- data.frame("feature"=geneFeatures$geneFeature, "id" = as.character(geneFeatures$featureId))
rownames(genes) <- genes[,1]
genes[,1] <- NULL
cat("\nRead ", nrow(genes), " feature id map data rows")
#head(genes)

# run DIU Analysis based on selected method - loop through all DBs and all Categories within each DB
results <- NULL
catResults <- NULL
len_zero = F
cont = 0
cont_err = 0
dbs <- ddply(geneFeatures, c("db"), summarise, N = length(result))
cat("DBs:\n")
print(head(dbs))
for(db in dbs$db) {
  gfDB <- geneFeatures[geneFeatures$db == db, ]
  dbCats <- ddply(gfDB, c("cat"), summarise, N = length(result))
  cat("DBCats:\n")        
  print(head(dbCats))
  for(category in dbCats$cat) {
    cont = cont + 1 
    dbcatIds <- geneFeatures[geneFeatures$db == db & geneFeatures$cat == category, ]
    #print(head(dbcatIds))
    dbcatVIds <- as.vector(dbcatIds[,c("geneFeature")])
    dbcatMatrixRaw <- featureMatrixRaw[dbcatVIds,]
    cat("DBCat matrixRaw: ", nrow(dbcatMatrixRaw), "\n")        
    #print(head(dbcatMatrixRaw))
    dbcatGenes <- data.frame("feature"=dbcatIds$geneFeature, "id" = as.character(dbcatIds$featureId))
    rownames(dbcatGenes) <- dbcatGenes[,1]
    dbcatGenes[,1] <- NULL
    cat("\nRead ", nrow(dbcatGenes), " feature id map data rows")
    #print(head(dbcatGenes))
    
    # run analysis for all entries in this source:feature
    if(method == "EDGER") {
      
      ### filter matrix
      cat("\nRead ", nrow(dbcatMatrixRaw), " expression data rows")
      
      dbcatMatrixRaw = dbcatMatrixRaw[order(rownames(dbcatMatrixRaw)),]
      
      if(!is.null(mff)){
        cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
        trans = minorFoldfilterTappas(dbcatMatrixRaw, genes[rownames(dbcatMatrixRaw),"id"], mff, minorMethod=filteringType)
        
        ### We need to remove the complementary isoforms
        aux = setdiff(1:nrow(dbcatMatrixRaw),which(rownames(dbcatMatrixRaw) %in% trans))
        ### if odd +1, if even -1
        res = c()
        for(number in aux){
          if(number %% 2 == 0)
            res = c(res, number, number-1)
          else
            res = c(res, number, number+1)
        }
        ### do unique for possible 1 and 2 remove and keep 1 2 2 1
        res = sort(unique(res))
        
        dbcatMatrixRaw=dbcatMatrixRaw[-res,]
      }
      ### end filter
      
      if(nrow(dbcatMatrixRaw) == 0){
        cont_err = cont_err + 1
        next
      }
      catResults = spliceVariant.DS(dbcatMatrixRaw, dbcatGenes, myfactors)
      } else {
      
      ### filter matrix
      cat("\nRead ", nrow(dbcatMatrixRaw), " expression data rows")
      
      dbcatMatrixRaw = dbcatMatrixRaw[order(rownames(dbcatMatrixRaw)),]
      
      if(!is.null(mff)){
        cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
        trans = minorFoldfilterTappas(dbcatMatrixRaw, genes[rownames(dbcatMatrixRaw),"id"], mff, minorMethod=filteringType)
        
        ### We need to remove the complementary isoforms (if delete without we also remove with)
        ### we look for isoforms we dont have to keep
        aux = setdiff(1:nrow(dbcatMatrixRaw),which(rownames(dbcatMatrixRaw) %in% trans))
        if(!length(aux)==0){
          ### we get its couple
          ### if odd +1, if even -1
          res = c()
          for(number in aux){
            if(number %% 2 == 0)
              res = c(res, number, number-1)
            else
              res = c(res, number, number+1)
          }
          ## remove the couple of isoforms
          ### do unique for possible 1 and 2 remove and keep 1 2 2 1
          res = sort(unique(res))
          dbcatMatrixRaw=dbcatMatrixRaw[-res,] 
        }
      }
      ### end filter
      
      cat(paste0(nrow(dbcatMatrixRaw), " isoforms remain.\n"))
      
      if(nrow(dbcatMatrixRaw) == 0){
        cont_err = cont_err + 1
        cat("For the category", as.character(category), "you have 0 transcripts to analyze.")
      }else{
        catResults = DEXSeq.DS(dbcatMatrixRaw, dbcatGenes, myfactors)
      }
    }
    
    if(is.null(catResults)){
      print("Your result matrix do not have results.")
      len_zero = T
      next
    }
    #print(head(catResults))
    results <- rbind(results, catResults)
  }
}
# if we dont have trans and we dont processed any feature before 

if((len_zero & cont_err == cont) | is.null(results)){

  cat("Final output\n")
  if(!is.null(results))
  write.table(results, file.path(outdir, paste0("dfi_result.",as.character(analysisId),".tsv")), quote=FALSE, row.names=FALSE, sep="\t")
  # write completion file
  cat("You have 0 transcripts to analyze with this feature/s.\n")

}else{
    cat("Preliminary final output\n")
    head(results)

    # get the podium change information
    pcList = podiumChange(featureMatrix, genes, myfactors)

    # need to merge the two sets of data into a single dataframe
    pcdf <- as.data.frame(pcList$podiumChange)
    pcdf["totalChange"] <- pcList$totalChange
    cat("Podium information\n")
    head(pcdf)
    result_dfi <- merge(results, pcdf, by=0)
    if(method == "EDGER") {
        colnames(result_dfi) <- c("gene", "pValue", "qValue", "secondCondition", "podiumChange", "totalChange")
    } else {
        colnames(result_dfi) <- c("gene", "qValue", "podiumChange", "totalChange")
    }
    cat("Final output\n")
    result_dfi <- data.frame(lapply(result_dfi, function(x) {gsub("GO__", "GO:", x)}))
    head(result_dfi)

    # write results file
    write.table(result_dfi, file.path(outdir, paste0("dfi_result.",as.character(analysisId),".tsv")), quote=FALSE, row.names=FALSE, sep="\t")

    # write completion file
    cat("\nWriting DFI Analysis completed file...")
    filedone <- file(file.path(outdir, paste0("done.",as.character(analysisId),".txt")))
    writeLines("done", filedone)
    close(filedone)

    # write sub dfiMatrixTest (subversion just to get PValues in tables)
    cat("\nWriting DFI Sub-Matrix file...")
    getGeneName <- function(x){
      res <- strsplit(as.character(x),";")[[1]][1]
      return(res)
    }

    getFeatureName <- function(x){
      res <- strsplit(as.character(x),";")[[1]][3]
      return(res)
    }

    ## TESTS

    lstTrans = strsplit(transFeatures, ",")
    lstProtein = strsplit(proteinFeatures, ",")
    dfi_total_count=read.table(file.path(inFileCount), sep="\t", header=TRUE, stringsAsFactors = F)

    cat("\nBuilding P-Values tables...")
    dfi_matrix = unique(result_dfi[,c("gene","qValue")])
    dfi_matrix$Feature = sapply(dfi_matrix$gene, getFeatureName)
    dfi_matrix$Gene = sapply(dfi_matrix$gene, getGeneName)
    dfi_matrix$DFI = ifelse(as.numeric(levels(dfi_matrix$qValue))[dfi_matrix$qValue]<=sig, "DFI", "Not DFI")

    dfi_results = data.frame()
    for(feature in unique(dfi_matrix$Feature)){
      newRow = data.frame(Feature=feature,
                          TotalCount=nrow(dfi_matrix[dfi_matrix$Feature==feature,]),
                          DFICount=nrow(dfi_matrix[dfi_matrix$Feature==feature & dfi_matrix$DFI == "DFI",]),
                          TotalAnnot=sum(dfi_total_count[dfi_total_count$Feature==feature,]$Total),
                          Level=ifelse(feature %in% lstTrans, "Transcript","Protein"),
                          Time="0")
      dfi_results = rbind(dfi_results,newRow)
    }

    dfi_results["Freq1"] = dfi_results[,"TotalCount"]/sum(dfi_results[,"TotalCount"])*100
    dfi_results["Freq2"] = dfi_results[,"TotalAnnot"]/sum(dfi_results[,"TotalAnnot"])*100
    dfi_results$Feature = as.character(dfi_results$Feature)
    dfi_results = dfi_results[order(dfi_results$Feature, method = ),]

    #########
    # TEST1 #
    #########

    # feature level
    result_utrmotif = NULL
    for(x in unique(dfi_results$Feature)){
      xDFI = dfi_results[dfi_results$Feature==x,]$DFICount
      xANNOT = dfi_results[dfi_results$Feature==x,]$TotalAnnot-xDFI
      allDFI = sum(dfi_results[dfi_results$Feature!=x,]$DFICount)
      allANNOT = sum(dfi_results[dfi_results$Feature!=x, ]$TotalAnnot)-allDFI

      tbDFI <- matrix(c(xDFI,xANNOT,allDFI,allANNOT),ncol=2,byrow=TRUE)
      colnames(tbDFI) <- c("DFI","ANNOT-DFI")
      rownames(tbDFI) <- c("X","ALL")
      tbDFI <- as.table(tbDFI)

      result_utrmotif = rbind(result_utrmotif, c(x,fisher.test(x = tbDFI, alternative="less")$p.value, as.character(tbDFI)))
    }
    result_utrmotif_df = data.frame(result_utrmotif, stringsAsFactors = F)
    result_utrmotif_df$X2 = as.numeric(result_utrmotif_df$X2)
    result_utrmotif_df$adj = p.adjust(result_utrmotif_df$X2, method = "hochberg")
    result_utrmotif_df = result_utrmotif_df[order(result_utrmotif_df$X1),]
    colnames(result_utrmotif_df) = c("Feature","P-Value","feat_DFI","all_DFI","feat_ANNOT-DFI","all_ANNOT-DFI", "Adj.P-Value")
    result_utrmotif_df$`P-Value` = signif(result_utrmotif_df$`P-Value`, digits = 5)
    result_utrmotif_df$`Adj.P-Value` = signif(result_utrmotif_df$`Adj.P-Value`, digits = 5)

    ##############
    # SAVE TESTS #
    ##############

    write.table(result_utrmotif_df, file = outputTestFeatures, quote = FALSE, col.names = TRUE, row.names = FALSE, sep = "\t")

    #########
    # TEST2 #
    #########

    # gene level
    cat("\nWriting DFI Test2 file...\n")

    res_genes = NULL
    dfi_matrix_genes = unique(dfi_matrix[,c("Gene","Feature","DFI")])
    for(gene in unique(dfi_matrix_genes$Gene)){
      for(feature in unique(dfi_matrix_genes[dfi_matrix_genes$Gene == gene,]$Feature)){
        submatrix = dfi_matrix_genes[dfi_matrix_genes$Gene == gene & dfi_matrix_genes$Feature == feature,]
        if(nrow(submatrix)>1) #a DFI and Not DFI
          res_genes = rbind(res_genes, submatrix[submatrix$DFI == "DFI",])
        else
          res_genes = rbind(res_genes, submatrix)
      }
    }

    dfi_results = data.frame()
    for(feature in unique(res_genes$Feature)){
      newRow = data.frame(Feature=feature,
                          TotalCount=nrow(res_genes[res_genes$Feature==feature,]),
                          DFICount=nrow(res_genes[res_genes$Feature==feature & res_genes$DFI == "DFI",]),
                          TotalAnnot=sum(dfi_total_count[dfi_total_count$Feature==feature,]$ByGenes),
                          Level=ifelse(feature %in% lstTrans, "Transcript","Protein"),
                          Time="0")
      dfi_results = rbind(dfi_results,newRow)
    }

    dfi_results["Freq1"] = dfi_results[,"TotalCount"]/sum(dfi_results[,"TotalCount"])*100
    dfi_results["Freq2"] = dfi_results[,"TotalAnnot"]/sum(dfi_results[,"TotalAnnot"])*100
    dfi_results$Feature = as.character(dfi_results$Feature)
    dfi_results = dfi_results[order(dfi_results$Feature, method = ),]

    #########
    # TEST2 #
    #########

    # gene level
    result_utrmotif = NULL
    for(x in unique(dfi_results$Feature)){
      xDFI = dfi_results[dfi_results$Feature==x,]$DFICount
      xANNOT = dfi_results[dfi_results$Feature==x,]$TotalAnnot-xDFI
      allDFI = sum(dfi_results[dfi_results$Feature!=x,]$DFICount)
      allANNOT = sum(dfi_results[dfi_results$Feature!=x, ]$TotalAnnot)-allDFI

      tbDFI <- matrix(c(xDFI,xANNOT,allDFI,allANNOT),ncol=2,byrow=TRUE)
      colnames(tbDFI) <- c("DFI","ANNOT-DFI")
      rownames(tbDFI) <- c("X","ALL")
      tbDFI <- as.table(tbDFI)

      result_utrmotif = rbind(result_utrmotif, c(x,fisher.test(x = tbDFI, alternative="less")$p.value, as.character(tbDFI)))
    }
    result_utrmotif_gene = data.frame(result_utrmotif, stringsAsFactors = F)
    result_utrmotif_gene$X2 = as.numeric(result_utrmotif_gene$X2)
    result_utrmotif_gene$adj = p.adjust(result_utrmotif_gene$X2, method = "hochberg")
    result_utrmotif_gene = result_utrmotif_gene[order(result_utrmotif_gene$X1),]
    colnames(result_utrmotif_gene) = c("Feature","P-Value","feat_DFI","all_DFI","feat_ANNOT-DFI","all_ANNOT-DFI", "Adj.P-Value")
    result_utrmotif_gene$`P-Value` = signif(result_utrmotif_gene$`P-Value`, digits = 5)
    result_utrmotif_gene$`Adj.P-Value` = signif(result_utrmotif_gene$`Adj.P-Value`, digits = 5)

    ##############
    # SAVE TESTS #
    ##############

    write.table(result_utrmotif_gene, file = outputTestGenes, quote = FALSE, col.names = TRUE, row.names = FALSE, sep = "\t")

    cat("\nAll done.\n")
}
