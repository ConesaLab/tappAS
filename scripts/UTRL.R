# UTR LEngthening Analysis R script
#
# UTR Lengthening analysis will be performed by using two different methods:
#   WILCOXON
#   masigpro??
#

library(plyr)
library("ggpubr")
library(tidyr)
library(ggplot2)
library(cowplot)
theme_set(theme_cowplot())

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

#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential PolyAdenylation Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 8)
stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""
outfile = ""
filterFC = ""
minLength = ""
filteringType = ""

for(i in 1:8) {
  if(length(grep("^-m", args[i])) > 0)
  method = substring(args[i], 3)
  else if(length(grep("^-i", args[i])) > 0)
  indir = substring(args[i], 3)
  else if(length(grep("^-o", args[i])) > 0)
  outdir = substring(args[i], 3)
  else if(length(grep("^-c", args[i])) > 0)
  outfile = substring(args[i], 3)
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

#### determine what type of data to run UTRL for
minLength = as.numeric(minLength)
groupNames <- strsplit(groupNames, ";")[[1]]

mff <- NULL
if(filterFC != "0") {
  mff = as.numeric(filterFC)
}

#method = "WILCOXON"
#indir = "./"
#utrdir = "./"
#outdir = "./"
#minLength = "100"
#filteringType = "PROP"
#mff = 0.1
#groupNames = c("NSC", "OLD")

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

# read info UTRL
cat("\nReading UTRL information data...")
infoUTRL = read.table(file.path(outdir, "utrl_info.tsv"), sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
cat("\nRead ", nrow(transMatrix), " UTRL data rows")

#Filter trasnMatrix - need genes and nrow(infoUTRL)<<transUTRL [select intersection - only 1<isoform and diff PAS]
genes_trans = c()
index = c()
count = 1

transMatrix = transMatrix[order(rownames(transMatrix)),]
cat("\nIntersecting UTRL information with transcript matrix...")
for(i in (rownames(transMatrix))){
  if(i %in% infoUTRL$Trans){
    genes_trans = c(genes_trans, infoUTRL$Gene[which(infoUTRL$Trans==i)])
    index = c(index, which(rownames(transMatrix)==i))
    count = count+1
  }
}

transMatrix = transMatrix[index,]
if(!is.null(mff)){
  cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
  trans = minorFoldfilterTappas(transMatrix, genes_trans, mff, minorMethod=filteringType)
  if(!is.null(trans))
  transMatrix=transMatrix[trans,]
}

infoUTRL = infoUTRL[which(infoUTRL$Trans %in% rownames(transMatrix)),c(1,2,3,4,5,6,7)]

#delete 1 transcript per gene after filtering
for(gene in unique(infoUTRL$Gene)){
  if(length(infoUTRL[infoUTRL$Gene==gene,"Gene"])==1)
  infoUTRL=infoUTRL[-which(infoUTRL$Gene==gene),]
}

#Tras filtrar, los porcentajes de expresión de cada isoforma no suman 100, deberías recalcularlos??

utrWeight3 = NULL
utrWeight5 = NULL
for(i in unique(infoUTRL$Gene)){
  gene = infoUTRL[infoUTRL$Gene==i,]
  utrWeight3[[i]] = apply(gene[,6:ncol(gene)], 2, function(x) weighted.mean(as.numeric(gene$UTR3),x))
  utrWeight5[[i]] = apply(gene[,6:ncol(gene)], 2, function(x) weighted.mean(as.numeric(gene$UTR5),x))
}

aux3 = data.frame(utrWeight3)
colnames(aux3) = names(utrWeight3)

aux5 = data.frame(utrWeight5)
colnames(aux5) = names(utrWeight5)

#####
# TEST 3UTR
#####
vector = NULL
gene = NULL
for(i in colnames(aux3)){
  vector = rbind(vector, aux3[1,i])
  vector = rbind(vector, aux3[2,i])
  gene = rbind(gene, i)
  gene = rbind(gene, i)
}

aux3_group <- data.frame(
group = rep(c(groupNames[1],groupNames[2]), ncol(aux3)),
weight = vector,
gene = gene
)

# run analysis
if(method == "WILCOXON") {
  res3 <- wilcox.test(weight ~ group, data = aux3_group, paired=TRUE, alternative = "less")
}
# 1.35e-05

utr3_result <- t(tidyr::spread(aux3_group, gene, weight))
colnames(utr3_result) <- c(groupNames)
utr3_result = as.data.frame(utr3_result[-1,])
utr3_result$gene = rownames(utr3_result)
len = ncol(utr3_result)
utr3_result = utr3_result[,c(len,1:len-1)]

#####
# TEST 5UTR
#####
vector = NULL
gene = NULL
for(i in colnames(aux5)){
  vector = rbind(vector, aux5[1,i])
  vector = rbind(vector, aux5[2,i])
  gene = rbind(gene, i)
  gene = rbind(gene, i)
}

aux5_group <- data.frame(
group = rep(c(groupNames[1],groupNames[2]), ncol(aux5)),
weight = vector,
gene = gene
)

# run analysis
if(method == "WILCOXON") {
  res5 <- wilcox.test(weight ~ group, data = aux5_group, paired=TRUE, alternative = "less")
}

utr5_result <- t(tidyr::spread(aux5_group, gene, weight))
colnames(utr5_result) <- c(groupNames)
utr5_result = as.data.frame(utr5_result[-1,])
utr5_result$gene = rownames(utr5_result)
len = ncol(utr5_result)
utr5_result = utr5_result[,c(len,1:len-1)]

#gene | g1 UTR3 | g2 UTR3 | g1 UTR5 | g2 UTR5
utr_result = cbind(utr3_result,utr5_result[c(2:len)])

#####
# Graphic
#####

#Diferencia de group2-group1

utrl_results3 <- data.frame(
group = rep(c("UTR3"), each = length(utrWeight3)),
weight = c(as.numeric(aux3[2,])-as.numeric(aux3[1,]))
)
utrl_results5 <- data.frame(
group = rep(c("UTR5"), each = length(utrWeight5)),
weight = c(as.numeric(aux5[2,])-as.numeric(aux5[1,]))
)

utrl_results = rbind(utrl_results3, utrl_results5)

minimum = min( #by 1st quartile
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[2,2], ":")[[1]][2])),
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[2,2], ":")[[1]][2]))
)

maximum = max( #by 3st quartile
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[5,2], ":")[[1]][2])),
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[5,2], ":")[[1]][2]))
)

m = max( #by interquartile range
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[5,2], ":")[[1]][2]))-as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[2,2], ":")[[1]][2])),
  as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[5,2], ":")[[1]][2]))-as.numeric(trimws(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[2,2], ":")[[1]][2]))
)

ggp <- ggplot(utrl_results, aes(x=group, y=round(weight), fill = group)) +
  geom_violin() +
  ylim(minimum-m,maximum+m) +
  #geom_blank() +
  stat_summary(fun.data=mean_sdl, geom="pointrange", color="black") +
  ylab(label = paste0("wUTR Difference (", groupNames[2], " - ", groupNames[1], ")")) +
  xlab(label = "UTR") +
  # Use custom color palettes
  scale_fill_manual(values=c("#f2635c", "#7cafd6")) +
  guides(fill=guide_legend(title="UTR")) +
  theme_bw()
#theme(panel.background = element_rect(fill = "white"))

# run analysis
if(method == "WILCOXON") {
  res <- wilcox.test(weight ~ group, data = utrl_results, exact = FALSE)
}

#0.0004626
head(res)

# write results file

res3$p.value = signif(res3$p.value, digits = 5)
res5$p.value = signif(res5$p.value, digits = 5)

pval_result = data.frame(
UTR = c("UTR3","UTR5"),
pval = c(res3$p.value,res5$p.value)
)

write.table(pval_result, file.path(outdir, "result_pval.tsv"), quote=FALSE, row.names=FALSE, sep="\t")

# write results file
write.table(utr_result, file.path(outdir, "result.tsv"), quote=FALSE, row.names=FALSE, sep="\t")
ggsave(outfile, plot=ggp, device="png", bg="transparent", width=7, height=7, dpi=250)
##########

# write completion file
cat("\nWriting UTRLAnalysis completed file...")
filedone <- file(file.path(outdir, "done.txt"))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
