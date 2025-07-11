# Differential PolyAdenylation Analysis R script
#
# Differential Isoform Usage analysis will be performed by using two different methods: 
#   edgeR spliceVariants function and
#   DEXSeq package
#
#

library(plyr)

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
  dxd <- estimateDispersions(dxd, BPPARAM = bp_param);
  ## Run the test and get results for exon bins 
  dxd <- testForDEU(dxd)
  ## Summarizing results on gene level
  cat("Get DEXSeqResults\n")
  res <- DEXSeqResults(dxd)
  #print(dim(res))
  #print(head(res))
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
    return (list(podiumChange = r, totalChange = sum(position$delta)/2, totalChange_1 = prop$cond1[2], totalChange_2 = prop$cond2[2]))
  }))

  A = unlist(sapply(changeOfPosition, function(x) x$podiumChange))
  B = unlist(sapply(changeOfPosition, function(x) x$totalChange))

  #save total_change for each condition to plot in heatmap
  C = unlist(sapply(changeOfPosition, function(x) x$totalChange_1))
  D = unlist(sapply(changeOfPosition, function(x) x$totalChange_2))
  
  d1 <- data.frame(row.names = names(C), matrix(C, nrow=length(C), byrow=T))
  d2 <- data.frame(row.names = names(D), matrix(D, nrow=length(D), byrow=T))
  d3 <- data.frame(row.names = names(A), matrix(A, nrow=length(A), byrow=T))

  d <- merge.data.frame(d1,d2, by = 0)
  rownames(d) = d$Row.names
  d <- d[,c(2,3)]
  d <- merge.data.frame(d,d3, by = 0)
  #this only works for case-control
  colnames(d) <- c("Gene", groupNames[1], groupNames[2], "Switching")
  write.table(d,file.path(outdir, "dpa_DPAU_ByCondition.tsv"), quote=FALSE, row.names=FALSE, sep="\t")

  return(list(podiumChange=A, totalChange=B))
}

#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential PolyAdenylation Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 7)
  stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""
filterFC = ""
minLength = ""
filteringType = ""
groupNames = ""
for(i in 1:7) {
    if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-f", args[i])) > 0)
      filterFC = substring(args[i], 3)
    else if(length(grep("^-l", args[i])) > 0)
      minLength = substring(args[i], 3)
    else if(length(grep("^-t", args[i])) > 0)
      filteringType = substring(args[i], 3)
    else if(length(grep("^-n", args[i])) > 0)
      groupNames = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(groupNames) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(minLength) == 0 || nchar(method) == 0 || nchar(filterFC) == 0 || nchar(filteringType) == 0)
  stop("Missing command line argument.")

#### determine what type of data to run DPA for
minLength = as.numeric(minLength)
groupNames <- strsplit(groupNames, ";")[[1]]

mff <- NULL
if(filterFC != "0") {
    mff = as.numeric(filterFC)
}

# read expression factors definition table
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "exp_factors.txt"), row.names=1, sep="\t", header=TRUE)
groups = length(unique(myfactors[,1]))
times = ncol(myfactors)

# read result matrix by transcripts
cat("\nReading normalized transcript matrix file data...")
transMatrix = read.table(file.path(indir, "result_matrix.tsv"), row.names=1, sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
#row.names(transMatrix) <- lapply(row.names(transMatrix), function(x) {gsub("GO:", "GO__", x)})
cat("\nRead ", nrow(transMatrix), " expression data rows")

# read info DPA
cat("\nReading DPA information data...")
infoDPA = read.table(file.path(outdir, "dpa_info.tsv"), sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
cat("\nRead ", nrow(transMatrix), " DPA data rows")

#Filter trasnMatrix - need genes and nrow(infoDPA)<<transDPA [select intersection - only 1<isoform and diff PAS]
genes_trans = c()
index = c()
count = 1

transMatrix = transMatrix[order(rownames(transMatrix)),]
cat("\nIntersecting DPA information with transcript matrix...")
for(i in (rownames(transMatrix))){
  if(i %in% infoDPA$Trans){
    genes_trans = c(genes_trans, infoDPA$Gene[which(infoDPA$Trans==i)])
    index = c(index, which(rownames(transMatrix)==i))
    count = count+1
  }
}

transMatrix = transMatrix[index,]
if(!is.null(mff)){
    cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
    trans = minorFoldfilterTappas(transMatrix, genes_trans, mff, minorMethod=filteringType)
    transMatrix=transMatrix[trans,]
}

infoDPA = infoDPA[which(infoDPA$Trans %in% rownames(transMatrix)),c(1,2,3,4)]

#delete 1 transcript per gene after filtering
for(gene in unique(infoDPA$Gene)){
  if(length(infoDPA[infoDPA$Gene==gene,"Gene"])==1)
    infoDPA=infoDPA[-which(infoDPA$Gene==gene),]
}

#Create L and S category [gene/trans/PAS/length_to_compare] #already checked if GenPos > length passed by user
cat("\nCreating categories for Distal and Proximal transcripts...")
df.infoDPA = data.frame(Gene = infoDPA$Gene, Trans = infoDPA$Trans, Pos = infoDPA$GenPos, Cat = "", stringsAsFactors = F)
for(gene in sort(unique(df.infoDPA$Gene))){
    # Strand varies the interpretation of pas position
    if(all(infoDPA[infoDPA$Gene==gene,]$Strand == "+")){
        maxPos = max(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
        minPos = min(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
        #just count genes with abs(maxPos-minPos>minLength)
        if(abs(maxPos-minPos)>minLength){
          for(trans in df.infoDPA[df.infoDPA$Gene==gene,]$Trans){
            transPos = df.infoDPA[df.infoDPA$Trans==trans,]$Pos
            df.infoDPA[df.infoDPA$Trans==trans,]$Cat = ifelse(abs(transPos-minPos)<abs(transPos-maxPos),"S","L")
          }
        }
    }else{
        maxPos = max(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
        minPos = min(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
        #just count genes with abs(maxPos-minPos>minLength)
        if(abs(maxPos-minPos)>minLength){
          for(trans in df.infoDPA[df.infoDPA$Gene==gene,]$Trans){
            transPos = df.infoDPA[df.infoDPA$Trans==trans,]$Pos
            df.infoDPA[df.infoDPA$Trans==trans,]$Cat = ifelse(abs(transPos-minPos)>abs(transPos-maxPos),"S","L")
          }
        }
    }
}

df.infoDPA = df.infoDPA[order(df.infoDPA$Gene),]
write.table(df.infoDPA,file.path(outdir, "dpa_cat.tsv"), quote=FALSE, row.names=FALSE, sep="\t")

# run DIU Analysis based on selected method
result_dpa <- NULL
trans_exprs <- NULL
trans_exprs_L <- matrix(, nrow = 0, ncol = ncol(transMatrix))
trans_exprs_S <- matrix(, nrow = 0, ncol = ncol(transMatrix))
names_L <- NULL
names_S <- NULL
names_genes <- NULL

#get transcript expression (just we need it)
df.infoDPA <- df.infoDPA[order(df.infoDPA$Cat),]

#infoDPA <- infoDPA[order(infoDPA$Category),]
for(gene in sort(unique(df.infoDPA$Gene))) {
  L_trans <- df.infoDPA[which(df.infoDPA$Gene==gene & df.infoDPA$Cat=="L"),]$Trans
  S_trans <- df.infoDPA[which(df.infoDPA$Gene==gene & df.infoDPA$Cat=="S"),]$Trans
  trans_exprs_L <- rbind(trans_exprs_L,apply(transMatrix[L_trans,],2,sum))
  trans_exprs_S <- rbind(trans_exprs_S,apply(transMatrix[S_trans,],2,sum))
  names_L <- c(names_L,paste0(gene,"_Long"))
  names_S <- c(names_S,paste0(gene,"_Short"))
  names_genes <- c(names_genes,gene,gene)
}

rownames(trans_exprs_L) <- names_L
rownames(trans_exprs_S) <- names_S
trans_exprs <- rbind(trans_exprs_L, trans_exprs_S[, colnames(trans_exprs_L)])
trans_exprs <- trans_exprs[order(rownames(trans_exprs)),]
names_genes <- data.frame(id = names_genes)
rownames(names_genes) <- rownames(trans_exprs)
colnames(names_genes) <- "id"

# run analysis
if(method == "EDGER") {
  result_dpa = spliceVariant.DS(trans_exprs, names_genes, myfactors)
} else {
  result_dpa = DEXSeq.DS(data.frame(trans_exprs), names_genes, myfactors)
}

head(result_dpa)

# get the podium change information
pcList = podiumChange(trans_exprs, names_genes, myfactors)

# need to merge the two sets of data into a single dataframe
pcdf <- as.data.frame(pcList$podiumChange)
pcdf["totalChange"] <- pcList$totalChange
cat("Podium information\n")
head(pcdf)
result_dpa <- merge(result_dpa, pcdf, by=0)
if(method == "EDGER") {
  colnames(result_dpa) <- c("gene", "pValue", "qValue", "secondCondition", "podiumChange", "totalChange")
} else {
  colnames(result_dpa) <- c("gene", "qValue", "podiumChange", "totalChange")
}
cat("Final output\n")
result_dpa <- data.frame(lapply(result_dpa, function(x) {gsub("GO__", "GO:", x)}))
head(result_dpa)

# write results file
write.table(result_dpa, file.path(outdir, "result.tsv"), quote=FALSE, row.names=FALSE, sep="\t")

###########
#Building expression matrix
###########

a <- data.frame(row.names = rownames(trans_exprs))
for(i in 1:(times*groups)){
  a[i] <- apply(trans_exprs[,(i*2-1):(i*2)],1,mean)
}

write.table(a, file.path(indir, "diff_PolyA_matrix_mean.tsv"), quote=FALSE, row.names=TRUE,col.names = FALSE, sep="\t")

##########

# write completion file
cat("\nWriting DPAnalysis completed file...")
filedone <- file(file.path(outdir, "done.txt"))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
