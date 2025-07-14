# Gene Set Enrichment Analysis R script
#
# Note: The enrichment analysis results, and adjusted PValues, are calculated within each category
#       All the category results are appended to the same results table which is the one saved to file
#
# GSEA will be performed by using two different methods: 
#   MDGSA package and
#   GOglm package

library(plyr)
library(dplyr)

### Filter GOterms
getGOFiltered <- function(cat2ids, rankedList, godir){
  cat("\nReading GO Ancestors data...")
  #all: rowId - featureId - featureDB - featureCat - featureDesc
  GO_Ancestors = read.table(godir, sep="\t", strip.white=TRUE, quote="\"", header=FALSE)
  GO_Ancestors$V2 <- gsub("[][ ]","",GO_Ancestors$V2) #delete "]", "[" and " "
  rownames(GO_Ancestors) <- GO_Ancestors$V1
  
  #intersection between cat2ids and ancestors
  subAnces <- GO_Ancestors[intersect(names(cat2ids),GO_Ancestors$V1),]
  
  #Filtering with TEST (intersection between genes test and all genes by GO)
  list_names <- rownames(rankedList)
  toDelete <- NULL
  cat("\nFiltering genes by test...")
  for(i in 1:length(cat2ids)){
    inter <- intersect(cat2ids[i][[1]],list_names)
    if(length(inter)>0){
      cat2ids[i][[1]] <- inter #carefully with the correct acces
    }else{
      toDelete <- c(toDelete,i)
    }
  }
  if(!is.null(toDelete)){
    cat2ids <- cat2ids[-toDelete]
  }
  #Filtering GOs
  cat("\nFiltering GOs...")
  GO_duplicates <- list()
  for(go in subAnces$V1){
    ancestors <- GO_Ancestors[which(GO_Ancestors$V1 == go),][2][[1]]
    ancestors <- strsplit(ancestors,",")[[1]]
    
    genes_go <- cat2ids[go]
    
    count = 1
    #if ancestors equals to 0 dont search
    if(!length(ancestors)==0){
      for(i in 1:length(ancestors)){
        if(is.element(ancestors[i],rownames(subAnces))){
          genes_ancestors<- cat2ids[ancestors[i]]
          if(setequal(genes_go[[1]],genes_ancestors[[1]])){ #dont use length to check
            GO_duplicates[ancestors[i]] <- c(go)
          }
        }
      }
    }
  }
  
  cat("\nTotal GOs filtered: ", length(GO_duplicates), "\n")
  #save(GO_duplicates, file="GO_duplicates.RData")
  #print(head(GO_duplicates))
  #print(head(names(GO_duplicates)))
  #cat2ids <- cat2ids[-which(names(cat2ids) %in% names(GO_duplicates))] #names are ancestors
  return(GO_duplicates)
}

### GOglm

goglm.GSEA <- function(rankedList, meanLength, featuresMap){
    library("GOglm")
    gsea_prepare <- prepare(rankedList, meanLength, trans.p = "d.log", trans.l = TRUE)
    cat("Prepared values:\n")
    #print(head(gsea_prepare))
    cat("Prepared values summary:\n")
    summary(gsea_prepare)
    print(plot(gsea_prepare))
    cat("Running GOglm...\n")
    goGlm_res <- goglm(gsea_prepare, featuresMap, n = 5)
    cat("Results summary:\n")
    summary(goGlm_res)
    goglmout <- cbind(goGlm_res$over.p, goGlm_res$anno, goGlm_res$rank)
    rownames(goglmout) <- unfactor(goGlm_res$GOID)
    colnames(goglmout) <- c("over.p", "n.anno", "rank")
    cat("GOglm Out:\n")
    print(head(goglmout))
    adj_df = data.frame(as.character(goGlm_res$GOID), goGlm_res$over.p)
    adj_df$adjusted_pvalue <- p.adjust(adj_df[,2], method = "BH")
    colnames(adj_df) <- c("feature", "overPValue", "adjPValue")
    return(adj_df)
}

### MDGSA

uvgsa.GSEA <- function(rankedList, featuresMap){
  library(mdgsa)
  pva.mdgsa.cutoff <- 0.001
  rankedList <- pval2index(rankedList)
  #annot <- annotMat2list (featuresMap)
  annot2 <- annotFilter(featuresMap, index = rankedList, minBlockSize = 10, maxBlockSize = 1000)
  res.uv <- uvGsa(rankedList, annot2)
  
  res.uv["feature"] <- rownames(res.uv)
  
  res.uv.sig = res.uv[res.uv$pval<pva.mdgsa.cutoff,]
  res.uv.sig[order(res.uv.sig$pval),]
  
  return(res.uv)
}

mdgsa.GSEA <- function(rankedList, featuresMap){
  library(mdgsa)
  pva.mdgsa.cutoff <- 0.001
  rankedList <- pval2index(rankedList)
  #annot <- annotMat2list (featuresMap)
  annot2 <- annotFilter(featuresMap, index = rankedList, minBlockSize = 10, maxBlockSize = 1000)
  res.md <- mdGsa(rankedList, annot2, fulltable = TRUE)
  
  res.md["feature"] <- rownames(res.md)
  
  #res.md.sig = res.md[res.md$pval<pva.mdgsa.cutoff,]
  #res.md.sig[order(res.md.sig$pval),]
  
  return(res.md)
}


#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential Isoform Usage Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 7)
  stop("Invalid number of arguments.")
dataType = ""
method = ""
aid = ""
indir = ""
outdir = ""
godir = ""
multiVariant = ""
for(i in 1:7) {
    if(length(grep("^-d", args[i])) > 0)
      dataType = substring(args[i], 3)
    else if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-a", args[i])) > 0)
      aid = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-g", args[i])) > 0)
      godir = substring(args[i], 3)
    else if(length(grep("^-v", args[i])) > 0)
      multiVariant = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(multiVariant) == 0 || nchar(outdir) == 0 || nchar(godir) == 0 || nchar(indir) == 0 || nchar(aid) == 0 || nchar(dataType) == 0 || nchar(method) == 0)
  stop("Missing command line argument.")

#### Read input data and run analysis

#read matrix to have all genes/proteins/trasncript
indir_project = substr(indir,1,nchar(indir)-5)
print(indir_project)

if(dataType == "gene")
    transMatrix = read.table(file.path(indir_project, "gene_matrix.tsv"), row.names=1, sep="\t", header=TRUE)
if(dataType == "trans")
    transMatrix = read.table(file.path(indir_project, "transcript_matrix.tsv"), row.names=1, sep="\t", header=TRUE)
if(dataType == "protein")
    transMatrix = read.table(file.path(indir_project, "protein_matrix.tsv"), row.names=1, sep="\t", header=TRUE)

# read ranked list 1
cat("\nReading ranked list entry values file data...")
filename = file.path(indir, paste(dataType, "_RLvalues.", "1.", aid, ".tsv", sep=""))
rankedList1 = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
cat("\nRead ", nrow(rankedList1), " ranked list entry values data rows")
head(rankedList1)

# read ranked list entries length 1
cat("\nReading ranked list entry lengths file data...")
filename = file.path(indir, paste(dataType, "_RLlengths.", "1.", aid, ".tsv", sep=""))
transLength1 = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
cat("\nRead ", nrow(transLength1), " ranked list entry length data rows")
head(transLength1)

if(multiVariant == "TRUE"){
    # read ranked list 2
    cat("\nReading ranked list entry values file data...")
    filename = file.path(indir, paste(dataType, "_RLvalues.", "2.", aid, ".tsv", sep=""))
    rankedList2 = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
    cat("\nRead ", nrow(rankedList2), " ranked list entry values data rows")
    head(rankedList2)

    # read ranked list entries length 2
    cat("\nReading ranked list entry lengths file data...")
    filename = file.path(indir, paste(dataType, "_RLlengths.", "2.", aid, ".tsv", sep=""))
    transLength2 = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
    cat("\nRead ", nrow(transLength2), " ranked list entry length data rows")
    head(transLength2)
}

# read features1
cat("\nReading features file data...")
filename = file.path(indir, paste(dataType, "_features.1.", aid, ".tsv", sep=""))

print(indir)
print(filename)

# featuresdata: rowId featureId featureDB featureCat featureDesc
# ALL ROWS IN FILE MUST BE FROM SAME FEATURE DB - it is likely to have duplicate row ids with different feature information
featuresData = read.table(filename, sep="\t", strip.white=TRUE, quote="\"", header=FALSE)

#keep the greater annotation list
if(multiVariant == "TRUE"){
    filename = file.path(indir, paste(dataType, "_features.2.", aid, ".tsv", sep=""))
    featuresData2 = read.table(filename, sep="\t", strip.white=TRUE, quote="\"", header=FALSE)
    #we have to merge both tables
    featuresData = rbind(featuresData, featuresData2)
    featuresData = featuresData[!duplicated(featuresData),]
    rm(featuresData2)

    transLength1$Gene = rownames(transLength1)
    transLength2$Gene = rownames(transLength2)

    transLength1 = rbind(transLength1, transLength2)
    transLength1 = transLength1[!duplicated(transLength1),]
    rownames(transLength1) = transLength1$Gene
}

# we are only interested in rowId featureId featureCat name/desc
featuresCats <- featuresData[, c(1,2,3,4,5)]
#just to don't have any error
index = -which(featuresCats$V1 %in% setdiff(featuresCats$V1,rownames(transLength1)))
if(length(index))
  featuresCats = featuresCats[-index,]

# summarize categories
cat("\nCategories summary...")
#cats <- ddply(featuresCats, c("V4"), summarise, N = length(result))
cats <- unique(featuresCats$V4)
head(featuresCats)
results <- NULL

# generate description lookup table
featuresDesc <- unique(featuresData[, c(2,5)])
names(featuresDesc) <- c("feature", "description")
print(head(featuresDesc))

# filtering GO with same genes if the feature is GeneOntology
featuresIds <- featuresCats[which(featuresData$V3 == "GeneOntology"), c(1,2)]
#Only if we have GOterms...
if(!length(rownames(featuresIds)) == 0){
    cat2ids= split(as.character(featuresIds$V1), list(as.character(featuresIds$V2)))

    cat("\nLength GOs before: ", length(unique(featuresCats$V2)), "\n")
    #GO_gene_before <- featuresCats
    #save(GO_gene_before, file="GO_gene_before.RData")

    GO_dup <- getGOFiltered(cat2ids, rankedList1, godir)
    featuresCats <- featuresCats[- which(featuresCats$V2 %in% names(GO_dup)),]

    cat("\nLength GOs after: ", length(unique(featuresCats$V2)), "\n")
    #GO_gene_after <- featuresCats
    #save(GO_gene_after, file="GO_gene_after.RData")
}

# process each category separately - there should be just a handful of categories
# each set of results is appended to the same, and only, results table
for(category in cats) {
    cat("\nProcessing feature: ", trimws(category), "\n")
    featuresCat <- featuresCats[trimws(featuresCats$V4) == trimws(category), ]
    print(head(featuresCat))
    cat("Category IDs: ")
    featuresIds <- featuresCat[, c(1,2)]
    print(head(featuresIds))

    cat2ids= split(as.character(featuresIds$V1), list(as.character(featuresIds$V2)))
    cat("Length featuresIds: ", nrow(featuresIds), ", ids: ", length(cat2ids))
    #cat("\nhead( IDs for", names(cat2ids)[1], "):\n")
    #print(head(cat2ids[[1]]))
    if(nrow(featuresIds) > 5) {
        print(head(cat2ids[[1]]))
        #need to use a different rankedList with pValues = 1
        if(method == "MDGSA") {
            ## MDGSA univariant
            if(multiVariant == "FALSE"){
                cat("\nUsing MDGSA\n")
                #If we don't use 1.0 pvalue to the rest of genes seems like works propertly
                #Add genes with 1.0 PValue
                #all <- rownames(transMatrix)
                #genes <- data.frame("gene"=as.character(unique(all[!(all %in% rownames(rankedList1))])), "pvalue"=1.0, stringsAsFactors=FALSE)
                #row.names(genes) <- genes$gen
                #genes <- subset(genes, select = c(pvalue))
                #colnames(rankedList1) <- c("pvalue") 
                #rankedListMDGSA = rbind(rankedList1, genes)

                cat("\nCalculating enrichment...\n")
                print(class(cat2ids))

                #Saving all GOs
                #save(cat2ids, file="ANNOT_GO.tsv")
                #print("Saved")
                catResults = uvgsa.GSEA(rankedList1, cat2ids)
            }else{
                ## multivariand
                cat("\nRunning MDGSA multivariant\n")

                print(class(cat2ids))

                rankedList1$Gene = rownames(rankedList1)
                rankedList2$Gene = rownames(rankedList2)
                merge_rank = full_join(rankedList1, rankedList2, by = "Gene", name = "pVal")
                rownames(merge_rank) = merge_rank$Gene
                merge_rank = merge_rank[,-2]
                colnames(merge_rank) = c("rankedList1", "rankedList2")
                merge_rank[is.na(merge_rank)] <- 1.0
                cat("\nCalculating enrichment...\n")
                catResults = mdgsa.GSEA(merge_rank, cat2ids)
            }
        } else {
            cat("\nUsing GOGLM\n")
            cat("\nCalculating enrichment...\n")
            catResults = goglm.GSEA(rankedList1, transLength1, cat2ids)
        }

        print("CatResults:")
        print(names(catResults))
        print(head(catResults))

        # add feature description and category columns
        cat("Number of different category IDs: ", nrow(catResults), "\n")
        if(nrow(catResults) > 0) {
            # must use !duplicated since the annotation file description may be different than
            # the description from GO term expansions which will result in multiple results for same term
            catResults <- merge(catResults, featuresDesc[!duplicated(featuresDesc$feature),], by="feature")
            print(table(catResults$feature %in% featuresDesc$feature))
            catResults["category"] <- trimws(category)
            cat("catResults\n")
            results <- rbind(results, catResults)
            print(head(results))

        }
        else {
          stop("No enrichment row results returned for feature: ", category)
        }
    }
    else {
        cat("Category does NOT have enough entries,", nrow(featuresIds), ", SKIPPING...\n\n")
    }
}
# name columns properly
resultsData <- NULL

if(method == "MDGSA") {
  if(multiVariant == "FALSE"){
    ##ADD NEW COLUMNS
    if(!is.null(results)) {
      if(nrow(results) > 0) {
        print(colnames(results))
        print(head(results))
        colnames(results) <- c("feature","N","lor", "overPValue", "adjPValue", "description", "category")
        resultsData <- results[, c(1,6,7,4,5)]
      }
    }
  }else{
    if(!is.null(results)) {
      if(nrow(results) > 0) {
        print(colnames(results))
        print(head(results))
        colnames(results) <- c("feature","N",
                               "lor.RankedList1","lor.RankedList2", "lor.I", 
                               "overPValue.RankedList1", "overPValue.RankedList2","overPValue.I",
                               "adjPValue.RankedList1", "adjPValue.RankedList2", "adjPValue.I",
                               "sd.rankedList1", "sd.rankedList2", "sd.I",
                               "t.rankedList1", "t.rankedList2", "t.I", 
                               "conv", "description", "category")
        resultsData <- results[, c(1,19,20,6,7,9,10)]
      }
    }
  }
}else{
  if(!is.null(results)) {
    if(nrow(results) > 0) {
      colnames(results) <- c("feature", "overPValue", "adjPValue", "description", "category")
      resultsData <- results[, c(1,4,5,2,3)]
    }
  }
}

#add source
options(warn=-1)
resultsData$source <- featuresCats$V3[which(resultsData$feature == as.character(levels(featuresCats$V2))[featuresCats$V2])][1]

#add number of genes with feature
numInCat = NULL
for(i in 1:length(resultsData$feature)){
  numInCat[i] = length(which(resultsData$feature[i] == as.character(levels(featuresCats$V2))[featuresCats$V2]))
}
options(warn=0)
#resultsData$numInCat <- length(which(resultsData$feature == as.character(levels(featuresCats$V2))[featuresCats$V2]))
resultsData$numInCat <- numInCat

# write results file
cat("\nWriting GSEA results file...")
write.table(resultsData, file.path(outdir, paste("result_", dataType, ".", aid, ".tsv", sep="")), quote=FALSE, row.names=FALSE, sep="\t")

# write completion file - use id for all files!!!
cat("\nWriting GSEA completed file...")
filedone <- file(file.path(outdir, paste("done_", dataType, ".", aid, ".txt", sep="")))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
