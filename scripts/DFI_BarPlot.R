# Generate scatter plot for DIU Analysis
#
# WARNING: This script is intended to be used directly by the tappAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input matrix: full path to matrix file
#   data type: TRANS, PROTEIN, GENE
#   output plot: full path to output plot image file
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by Pedro Salguero - Lorena de la Fuente

library("ggplot2")
library("ggrepel")
library("plyr")
library("stringr")

round_df <- function(x, digits) {
  # round all numeric variables
  # x: data frame 
  # digits: number of digits to round
  numeric_columns <- sapply(x, mode) == 'numeric'
  x[numeric_columns] <-  round(x[numeric_columns], digits)
  x
}

# handle command line arguments - don't want to use external package so users don't have to install it
args = commandArgs(TRUE)
cat("%Features plots arguments: ", args, "\n")
arglen = length(args)
if(arglen != 11){
    print(arglen)
    stop("Invalid number of arguments.")
}
inFile = ""
inFileCount = ""
outFile1 = ""
outFile2 = ""
outFile3 = ""
outFile4 = ""
outputTestFeatures = ""
outputTestGenes = ""
analysisId = ""
transFeatures = ""
proteinFeatures = ""
for(i in 1:11) {
  if(length(grep("^-i", args[i])) > 0)
    inFile = substring(args[i], 3)
  else if(length(grep("^-o1", args[i])) > 0)
    outFile1 = substring(args[i], 4)
  else if(length(grep("^-o2", args[i])) > 0)
    outFile2 = substring(args[i], 4)
  else if(length(grep("^-o3", args[i])) > 0)
    outFile3 = substring(args[i], 4)
  else if(length(grep("^-o4", args[i])) > 0)
    outFile4 = substring(args[i], 4)
  else if(length(grep("^-c", args[i])) > 0)
    inFileCount = substring(args[i], 3)
  else if(length(grep("^-a", args[i])) > 0)
    analysisId = substring(args[i], 3)
  else if(length(grep("^-t1", args[i])) > 0)
    outputTestFeatures = substring(args[i], 4)
  else if(length(grep("^-t2", args[i])) > 0)
    outputTestGenes = substring(args[i], 4)
  else if(length(grep("^-lt", args[i])) > 0)
    transFeatures = substring(args[i], 4)
  else if(length(grep("^-lp", args[i])) > 0)
    proteinFeatures = substring(args[i], 4)
  else {
    cat("Invalid command line argument: '", args[i], "'\n")
    stop("Invalid command line argument.")
  }
}
if(nchar(transFeatures) == 0 || nchar(proteinFeatures) == 0 || nchar(analysisId) == 0 || nchar(outFile1) == 0 || nchar(outFile2) == 0 || nchar(outFile3) == 0 || nchar(outFile4) == 0 || nchar(inFile) == 0 || nchar(inFileCount) == 0 || nchar(outputTestFeatures) == 0 || nchar(outputTestGenes) == 0)
  stop("Missing command line argument.")

cat("\nReading data matrix...")
dfi_matrix=read.table(file.path(inFile), sep="\t", header=TRUE, stringsAsFactors = F)
dfi_total_count=read.table(file.path(inFileCount), sep="\t", header=TRUE, stringsAsFactors = F)
#dfi_total_count=dfi_total_count[!duplicated(dfi_total_count), ]

myPalette = c("#f2635c","#7cafd6","#a2ca72","#f7c967","#f9f784","#a16cc1","#a796ff","#ff96eb")

##lstProtein = c("ACT_SITE","BINDING","COILED","COMPBIAS","DISORDER","DOMAIN","INTRAMEM","MOTIF","PTM","SIGNAL","TRANSMEM")
##lstTrans = c("3UTRmotif","5UTRmotif","RNA_binding","uORF","miRNA")

lstTrans = strsplit(transFeatures, ",")
lstProtein = strsplit(proteinFeatures, ",")

intersecTrans = dfi_total_count$Feature %in% unlist(lstTrans)
intersecProt = dfi_total_count$Feature %in% unlist(lstProtein)
featuresUsed = Reduce("|", list(intersecTrans,intersecProt))


if(!length(which(featuresUsed == FALSE))==0)
    dfi_total_count = dfi_total_count[-which(featuresUsed == FALSE),]

#########
# PLOT1 #
#########

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

ggp1 = ggplot(dfi_results) +
    geom_bar(aes(x=Feature,y=Freq2,fill=as.factor(Time)),width =0.8, size=0.5,
    color="black", stat = "identity", position = position_dodge()) +
    geom_line(aes(x=Feature,y=Freq1), stat="identity", group = 1) +
    geom_point(aes(x=Feature,y=Freq1, shape="Features"), stat="identity", group = 1) +
    ylab("% Features") +
    xlab("Category") +
    labs(fill= "", shape=NULL) +
    scale_fill_manual(labels = c("DFI Features"), values = c(myPalette[c(2)])) +
    theme_classic() +
    theme(axis.title.x = element_text(size=17, margin=margin(5,0,0,0)),
    axis.text.x  = element_text(margin=margin(7,0,0,0), size=17, hjust = 1, angle = 45),
    axis.title.y = element_text(size=17,  margin=margin(0,15,0,0)),
    axis.text.y  = element_text(vjust=0.5, size=17))  +
    facet_grid(~ Level, scales = "free",  space="free") +  theme(strip.text.x = element_text(size = 15, face="bold"), strip.background = element_rect(colour="black", fill=c("white")))

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
# PLOT2 #
#########

res_genes = NULL
dfi_matrix_genes = unique(dfi_matrix[,c(1,2,4)])
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

ggp2 = ggplot(dfi_results) +
    geom_bar(aes(x=Feature,y=Freq2,fill=as.factor(Time)),width =0.8, size=0.5, color="black", stat = "identity", position = position_dodge()) +
    geom_line(aes(x=Feature,y=Freq1), stat="identity", group = 1) +
    geom_point(aes(x=Feature,y=Freq1, shape="Features Genes"), stat="identity", group = 1) +
    ylab("% Features Genes") +
    xlab("Category") +
    labs(fill= "", shape=NULL) +
    scale_fill_manual(labels = c("DFI Genes"), values = myPalette[c(3)]) +
    theme_classic() +
    theme(axis.title.x = element_text(size=17, margin=margin(5,0,0,0)),
    axis.text.x  = element_text(margin=margin(7,0,0,0), size=17, hjust = 1, angle = 45),
    axis.title.y = element_text(size=17,  margin=margin(0,15,0,0)),
    axis.text.y  = element_text(vjust=0.5, size=17))  +
    facet_grid(~ Level, scales = "free",  space="free") +  theme(strip.text.x = element_text(size = 15, face="bold"), strip.background = element_rect(colour="black", fill=c("white")))

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

#########
# PLOT3 #
#########

dfFavored = dfi_matrix[dfi_matrix$DFI == "DFI",c("Feature","Favored")]
total_favored = data.frame()
single = FALSE
#Single Time Series - we have to not accept "," in Group titles
if(length(which(str_detect(dfFavored[,"Favored"], ",")==T))>0){
    single = TRUE
    aux = dfFavored[str_detect(dfFavored[,"Favored"], ","),]
    splited = data.frame()
    for(i in 1:nrow(aux)){
        times = str_split_fixed(aux$Favored[i], ",", Inf)
        for(j in times){
            nrow = data.frame(Feature=aux$Feature[i],
            Favored=j)

            splited = rbind(splited, nrow)
        }
    }

    dfFavored = dfFavored[-which(str_detect(dfFavored[,"Favored"], ",")),]
    dfFavored = rbind(dfFavored,splited)
    dfFavored = dfFavored[order(as.character(dfFavored$Favored)),]

}

for(feature in unique(dfFavored$Feature)){
    group = unique(dfFavored$Favored)
    for(name in group){
        newRow = data.frame(
        "Feature" = feature,
        "Favored" = name,
        "Count" = nrow(dfFavored[dfFavored$Feature==feature & dfFavored$Favored==name,]),
        "Level" = ifelse(lapply(lstTrans, function(x) feature %in% x), "Transcript","Protein")
        )
        total_favored = rbind(total_favored,newRow)
    }
    total_favored[total_favored$Feature==feature,]$Count <- (total_favored[total_favored$Feature==feature,]$Count/sum(total_favored[total_favored$Feature==feature,]$Count))*100
}

if(length(which(total_favored$Favored=="N/A"))>0)
total_favored = total_favored[-which(total_favored$Favored=="N/A"),]

total_favored = total_favored[order(total_favored$Level, method = ),]

if(single){
    ggp3 = ggplot(total_favored, aes(x=Feature, y = Count, fill=Favored)) +
        geom_bar(width = 0.5, color="black", stat = "identity") +
        coord_flip() +
        theme_classic() +
        theme(axis.title.x = element_text(size=17, margin=margin(5,0,0,0)),
        axis.text.x  = element_text(margin=margin(7,0,0,0), size=17, hjust = 1, angle = 45),
        axis.title.y = element_text(size=17,  margin=margin(0,15,0,0)),
        axis.text.y  = element_text(vjust=0.5, size=17))  + xlab("Category") +
        facet_grid(~ Favored, scales = "fixed",  space="free", shrink = TRUE) +
        theme(strip.text.x = element_text(size = 15, face="bold"), strip.background = element_rect(colour="black", fill=c("white"))) +
        scale_fill_manual(values = c(myPalette[2], myPalette[1], myPalette[3], myPalette[4], myPalette[5])) +
        ylab("% DFI features")
}else{
    ggp3 = ggplot(total_favored, aes(x=Feature, y = Count, fill=Favored)) +
        geom_bar(width = 0.5, color="black", stat = "identity", position = "fill") +
        theme_classic() +
        theme(axis.title.x = element_text(size=17, margin=margin(5,0,0,0)),
        axis.text.x  = element_text(margin=margin(7,0,0,0), size=17, hjust = 1, angle = 45),
        axis.title.y = element_text(size=17,  margin=margin(0,15,0,0)),
        axis.text.y  = element_text(vjust=0.5, size=17))  + xlab("Category") +
        facet_grid(~ Level, scales = "free",  space="free") +
        theme(strip.text.x = element_text(size = 15, face="bold"), strip.background = element_rect(colour="black", fill=c("white"))) +
        geom_hline(yintercept=0.5, linetype="dashed",
        color = myPalette[1], size=1)+
        scale_fill_manual(values = c(myPalette[2], myPalette[1], myPalette[3], myPalette[4], myPalette[5])) +
        ylab("% DFI features")
}

#########
# PLOT4 #
#########

totalOLD_MOT_mDS = dfi_matrix[dfi_matrix$DFI == "DFI",c("Feature","TotalChange")]
totalOLD_MOT_mDS$Level = ifelse(totalOLD_MOT_mDS$Feature %in% unlist(lstTrans), "Transcript","Protein")
totalOLD_MOT_mDS = totalOLD_MOT_mDS[order(totalOLD_MOT_mDS$Feature, method = ),]

ggp4 = ggplot(totalOLD_MOT_mDS, aes(x=Feature, y=abs(TotalChange))) +
    geom_boxplot( fill=myPalette[6], alpha=0.8) +
    theme_classic() +
    scale_y_continuous(limits = c(0,max(abs(totalOLD_MOT_mDS$TotalChange))*1.1), expand = c(0,0))+
    scale_fill_manual(values = myPalette[3]) +
    labs( y = expression(paste("Feature Total Change ( ", Delta, "FI)"))) +
    theme(axis.title.x = element_text(size=17, margin=margin(5,0,0,0)),
    axis.text.x  = element_text(margin=margin(7,0,0,0), size=17, hjust = 1, angle = 45),
    axis.title.y = element_text(size=17,  margin=margin(0,15,0,0)),
    axis.text.y  = element_text(vjust=0.5, size=17))  + xlab("Category") +
    facet_grid(~ Level, scales = "free",  space="free")+
    theme(strip.text.x = element_text(size = 15, face="bold"), strip.background = element_rect(colour="black", fill=c("white"))) +guides(fill=FALSE)

#kruskal.test(x = list(valores))$p.value

##############
# SAVE PLOTS #
##############

ggsave(outFile1, plot=ggp1, device="png", bg="transparent", width=10, height=7.5, dpi=120)
ggsave(outFile2, plot=ggp2, device="png", bg="transparent", width=10, height=7.5, dpi=120)
ggsave(outFile3, plot=ggp3, device="png", bg="transparent", width=10, height=7.5, dpi=120)
ggsave(outFile4, plot=ggp4, device="png", bg="transparent", width=10, height=7.5, dpi=120)

cat("\nAll done.\n")
