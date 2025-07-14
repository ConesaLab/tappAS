#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments

# FEA script arguments:
  # -d - data type
  # -f - feature analysis id
  # -m - method: "Wallenious", "Sampling", "Hypergeometric"
  # -s - number of samples (only applicable for 'sampling')
  # -u - use w/o category flag: "y", "n"
  # -i - input folder
  # -o - output folder
  # -g - goAncestors file

library(plyr)

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
  list_names <- rankedList
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




##################################################
############ GOseq function ######################
##################################################

# returns enrichment result
trim <- function (x) {
    gsub("^\\s+|\\s+$", "", x)
}
goseq.FEA <- function(testItems, bkgndItems, featuresData, itemLengths, statMethod, nscnt, busewoc) {
  library("goseq")

  # create array with 0s for all isoforms
  allItems <- bkgndItems
  cat("\nTotal number of items: ", length(allItems))
  cat("\nNumber of test items: ", length(testItems), "\n")
  iso.goseq = as.integer(allItems %in% testItems)
  names(iso.goseq) = allItems
  print(head(iso.goseq))
  # PB.100.1 PB.1001.1 PB.1005.2 PB.1007.1 PB.1010.1 PB.1033.1 
  #      0         0         0         0         0         0 

  # get length for all allItems
  itemLengths = itemLengths[names(iso.goseq),]
  names(itemLengths) = names(iso.goseq)
  print(head(itemLengths))
  # PB.100.1 PB.1001.1 PB.1005.2 PB.1007.1 PB.1010.1 PB.1033.1 
  #   1667      2052      5070      2597      1977      4182 
  
  # calculate the Probability Weighting Function or PWF which can be thought of as a function 
  # which gives the probability that a gene will be differentially expressed (DE), based on its length alone.
  mipwf <- result_matrix <- NULL
  mipwf <- nullp(iso.goseq, bias.data = itemLengths, plot.fit=FALSE)
  print(head(mipwf))
  #             DEgenes bias.data  pwf
  # PB.100.1        0      1667 0.5331029
  # PB.1001.1       0      2052 0.5667801

  # now do the real calculations, add adjusted value, and significant columns
  # > head(result_matrix)
  #      category        over_represented_pvalue  under_represented_pvalue numDEInCat numInCat         term         ontology adjusted_pvalue significant
  # 2452 GO:0008270            2.547081e-09                1.0000000        465      676         zinc ion binding       MF    2.341607e-05         yes
  # 1679 GO:0006099            4.628597e-09                1.0000000         48       51 tricarboxylic acid cycle       BP    2.341607e-05         yes
  result_matrix <- goseq(pwf = mipwf, gene2cat = featuresData, method = statMethod, repcnt = nscnt, use_genes_without_cat=busewoc)
  result_matrix$adjusted_pvalue <- p.adjust(result_matrix$over_represented_pvalue, method = "BH") 
  print(head(result_matrix))
  if(ncol(result_matrix) == 8) {
    cat("Removing GO term data from goseq results")
    result_matrix <- result_matrix[, c(1,2,3,4,5,8)]
    print(head(result_matrix))
  }
  colnames(result_matrix) <- c("feature", "overPValue", "underPValue", "numDEInCat", "numInCat", "adjPValue")
  return(result_matrix)
}

# validate command line arguments
args = commandArgs(TRUE)
cat("Functional Enrichment Analysis script arguments: ", args)
arglen = length(args)
if (arglen != 8)
  stop("\nInvalid number of arguments.")

method = ""
scnt = ""
usewoc = ""
dataType = ""
fid = ""
indir = ""
outdir = ""
godir = ""

for(i in 1:8) {
    if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-s", args[i])) > 0)
      scnt = substring(args[i], 3)
    else if(length(grep("^-u", args[i])) > 0)
      usewoc = substring(args[i], 3)
    else if(length(grep("^-d", args[i])) > 0)
      dataType = substring(args[i], 3)
    else if(length(grep("^-f", args[i])) > 0)
      fid = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-g", args[i])) > 0)
      godir = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(method) == 0 || nchar(scnt) == 0 || nchar(usewoc) == 0 || nchar(outdir) == 0 || nchar(godir) == 0 ||nchar(indir) == 0 || nchar(fid) == 0 || nchar(dataType) == 0)
    stop("\nMissing command line argument.")
nscnt = as.numeric(scnt)
busewoc = as.numeric(usewoc)

# read test list
cat("\nReading test list entry values file data...")
filename = file.path(indir, paste(dataType, "_test.", fid, ".tsv", sep=""))
#testList = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
testList <- scan(filename, what="", sep="\n")
cat("\nRead ", nrow(testList), " test list entry values data rows")
head(testList)

# read background list
cat("\nReading background list entry values file data...")
filename = file.path(indir, paste(dataType, "_bkgnd.", fid, ".tsv", sep=""))
#bkgndList = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
bkgndList <- scan(filename, what="", sep="\n")
cat("\nRead ", nrow(bkgndList), " background list entry values data rows")
head(bkgndList)

# read items length
cat("\nReading items lengths file data...")
filename = file.path(indir, paste(dataType, "_lengths.", fid, ".tsv", sep=""))
itemLengths = read.table(filename, row.names=1, sep="\t", strip.white=TRUE, header=FALSE)
cat("\nRead ", nrow(itemLengths), " items length data rows")
head(itemLengths)

# read features
cat("\nReading features file data...")
filename = file.path(indir, paste(dataType, "_features.", fid, ".tsv", sep=""))
print(filename)
# featuresdata: rowId featureId featureDB featureCat featureDesc
featuresData = read.table(filename, sep="\t", strip.white=TRUE, quote="\"", header=FALSE)
cat("\nRead ", nrow(featuresData), " features data rows")
head(featuresData)

# we are only interested in rowId featureId featureCat name/desc
featuresCats <- featuresData[, c(1,2,4,5)]
# summarize categories
cat("\nCategories summary...")
cats <- ddply(featuresCats, c("V4"), summarise, N = length(result))
print(head(featuresCats))
results <- NULL

# generate description lookup table
featuresDesc <- unique(featuresData[, c(2,5)])
names(featuresDesc) <- c("feature", "description")
print(head(featuresDesc))

# generate Source lookup table
featuresSource <- unique(featuresData[, c(2,3)])
names(featuresSource) <- c("feature", "source")
print(head(featuresSource))

# filtering GO with same genes if the feature is GeneOntology
featuresIds <- featuresCats[which(featuresData$V3 == "GeneOntology"), c(1,2)]
#Only if we have GOterms...
if(!length(rownames(featuresIds)) == 0){
    cat2ids= split(as.character(featuresIds$V1), list(as.character(featuresIds$V2)))

    cat("\nLength GOs before: ", length(unique(featuresCats$V2)), "\n")
    #GO_gene_before <- featuresCats
    #save(GO_gene_before, file="GO_gene_before.RData")

    GO_dup <- getGOFiltered(cat2ids, testList, godir)
    featuresCats <- featuresCats[- which(featuresCats$V2 %in% names(GO_dup)),]

    cat("\nLength GOs after: ", length(unique(featuresCats$V2)), "\n")
    #GO_gene_after <- featuresCats
    #save(GO_gene_after, file="GO_gene_after.RData")
}

# process each category separately - there should be just a handful of categories
# each set of results is appended to the same, and only, results table
for(category in cats$V4) {
    cat("\nProcessing feature: ", trimws(category), "\n")
    featuresCat <- featuresCats[trimws(featuresCats$V4) == trimws(category),]
    print(head(featuresCat))
    featuresIds <- featuresCat[, c(1,2)]
    print(head(featuresIds))

    # calculate enrichment
    cat("\nCalculating functional enrichment...")
    catResults = goseq.FEA(testList, bkgndList, featuresIds, itemLengths, method, nscnt, busewoc)

    # add feature description and category columns
    cat("Number of different category IDs: ", nrow(catResults), "\n")
    if(nrow(catResults) > 0) {
        # must use !duplicated since the annotation file description may be different than
        # the description from GO term expansions which will result in multiple results for same term
        catResults <- merge(catResults, featuresDesc[!duplicated(featuresDesc$feature),], by="feature")
        catResults <- merge(catResults, featuresSource, by="feature")
        catResults["category"] <- trimws(category)
        cat("catResults\n")
        print(head(catResults))
        results <- rbind(results, catResults)
        print(head(results))
    }
    else {
      stop("No enrichment rows found for feature: ", category)
    }
}
# name columns properly
resultsData <- NULL
if(nrow(results) > 0) {
    colnames(results) <- c("feature", "overPValue", "underPValue", "numDEInCat", "numInCat", "adjPValue", "description","source", "category")
    resultsData <- results
    #Comment to get more significant genes to cluster
    #resultsData[,"adjPValue"] = resultsData[,"overPValue"]
}

# save results
cat("\nWriting FEA results file...")
write.table(resultsData, file.path(outdir, paste("result_", dataType, ".", fid, ".tsv", sep="")), quote=FALSE, row.names=FALSE, sep="\t")
cat("\nWriting analysis completed file...")
filedone <- file(file.path(outdir, paste("done_", dataType, ".", fid, ".txt", sep="")))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
