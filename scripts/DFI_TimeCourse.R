# Differential Feature Inclusion Analysis R script
#
# Differential Isoform Usage analysis will be performed using maSigPro 
#

#
# maSigPro custom changes - should be incorporated into R package later
#

library("maSigPro")
library("MASS")
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


  IsoModel_tappas <- function(data, gen, design = NULL, degree = 2, Q = 0.05, min.obs = 6, minorFoldfilter = NULL, 
                       counts = FALSE, family = NULL, theta = 10, epsilon = 1e-05, triple = FALSE) 
  {
    #---------------------------------------------------------------------------------------------------
    # data is a matrix containing isoform expression. Isoforms must be in rows and experimental conditions in columns
    # gen is a vector with the name of the gene each isoform belongs to
    #---------------------------------------------------------------------------------------------------
    
    Genes <- unique(gen)
    g <- length(Genes)
    
    if (is.null(family)) {
      if (!counts) {
        family = gaussian()
      }
      if (counts) {
        family = negative.binomial(theta)
      }
    }
    
    #---------------------------------------------------------------------------------------------------
    # STEP -1: Remove cases with low expressed isoforms:
    #---------------------------------------------------------------------------------------------------
    
    print (paste(nrow(data), "transcripts"))
    print (paste(length(unique(gen)), "genes"))

    totaldata = data  #### NEW!
    totalgen = gen   #### NEW!
    
    if (!is.null(minorFoldfilter)) {
      print ("Removing low expressed minor isoforms")
      moreOne <- names(which(table(gen) > 1))
      iso.sel <- NULL
      gene.sel <- NULL
      for ( i in moreOne) {
        which(gen==i)
        gene.data <- data[which(gen==i),]
        isoSUM <- apply(gene.data, 1, sum)
        major <- names(which(isoSUM == max(isoSUM)))[1]
        minors <- names(which(isoSUM != max(isoSUM)))
        div <- as.numeric(matrix(rep(gene.data[major,], length(minors)), ncol = ncol(data), length(minors), byrow = T)) / as.matrix(gene.data[minors,])
        is <- names(which(apply(div, 1, min, na.rm = T) < minorFoldfilter))
        iso.sel <- c(iso.sel, major, is)
        gene.sel <- c(gene.sel, rep(i, length(is)+1))
      }
      
      data <- data[iso.sel,]
      gen <- gene.sel
      print(dim(data))
      print(length(gen))
      print ("Done")
    }
    
    #---------------------------------------------------------------------------------------------------
    #  STEP 0: Remove cases with 1 transcript:
    #---------------------------------------------------------------------------------------------------
    
    NT <- tapply(rownames(data),gen,length)
    Genes1 <- names(which(NT!=1))
    data1 <- data[gen%in%Genes1,]
    gen1 <- gen[gen%in%Genes1]
    Genes1 <- unique(gen1)
    print (paste(nrow(data1), "remaining transcripts to analyse DS"))   # changed
    print (paste(length(unique(Genes1)), "remaining genes to analyse DS")) # changed

    #---------------------------------------------------------------------------------------------------
    # STEP 1: Gene models comparison. 
    #---------------------------------------------------------------------------------------------------
  
    # make.design.matrix 
    results = NULL
    
    if (ncol(design$edesign)==3){
      results <- modelIso(Formula=Formula0, design, family, data1, Genes1, epsilon, Q, gen1)
    } 
    else{
      # for (k in 3:ncol(design$edesign)){
      #   singleG_dis = design$edesign[which(design$edesign[,k]==1), c(1,2,k)]
      #   dis = make.design.matrix(singleG_dis, degree = degree)
      #   results[[colnames(design$edesign)[k]]] <- function(Formula=Formula0, dis, family, data1, Genes1, epsilon, Q)
      # }
      if(triple){Formula=Formula000} else {Formula=Formula00}
        results <- modelIso(Formula=Formula, design, family, data1, Genes1, epsilon, Q, gen1)
    }
        
  
    #---------------------------------------------------------------------------------------------------
    # STEP 2: p.vector and T.fit for DE to the transcripts that belong to selected.genes
    #---------------------------------------------------------------------------------------------------
    # data2 <- data[gen%in%selected.genes,]
    # gen2 <- gen[gen%in%selected.genes]
    # pvector2 <- p.vector(data2, design, counts=counts, item="isoform")
    # Tfit2 <- T.fit(pvector2, item="isoform")
    # 
    #---------------------------------------------------------------------------------------------------
    # Output  
    #---------------------------------------------------------------------------------------------------
    
    #ISO.SOL <-list(data, gen, design, selected.genes, pvector2, Tfit2)
    #names(ISO.SOL)<- c("data","gen", "design", "DSG","pvector","Tfit")
    #ISO.SOL

    
    ISO.SOL <-list(totaldata, totalgen, design, results$selected.genes, results$p.adjusted)
    names(ISO.SOL)<- c("data","gen", "design", "DSG", "adjpvalue")
    ISO.SOL  
    
    }
  
  
  #---------------------------------------------------------------------------------------------------
  # Auxiliar internal functions: Model, REP, Formula0, Formula1  
  #---------------------------------------------------------------------------------------------------
  
  modelIso <- function(Formula, design, family, data1, Genes1, epsilon, Q, gen1){
    pval<-NULL
    g <- length(Genes1)
    dis <- as.data.frame(design$dis)
    mycolnames <- colnames(dis)
    for(i in 1:g)
    {
      div <- c(1:round(g/100)) * 100
      if (is.element(i, div)) 
        print(paste(c("fitting gene", i, "out of", g), collapse = " "))
      
      zz <-data1[gen1==Genes1[i],]
      nt <- nrow(zz) 
      
      dis.gen <- REP(dis,nt)
      y <-  c(t(as.matrix(zz)))
      transcript<- factor(rep(c(1:nt),each = ncol(zz)))
      ydis <- cbind(y, dis.gen, transcript)
      
      model0 <- glm(Formula(mycolnames,design$edesign), data=ydis,  family = family, epsilon = epsilon )
      model1 <- glm(Formula1(mycolnames), data=ydis,  family = family, epsilon = epsilon )

      if(family$family=="gaussian")  {
        pvali <- anova(model0, model1, test="F")[2,6] }
      else {
        pvali <- anova(model0,model1, test = "Chisq")[2,5] }
      names(pvali) <- Genes1[i]
      pval <- c(pval, pvali)
    }
    p.adjusted <- p.adjust(pval, method="fdr")
    num.genes <- sum(p.adjusted<Q, na.rm=TRUE)
    selected.genes <-names(sort(p.adjusted)[1:num.genes])
    
    results = list(p.adjusted, selected.genes)
    names(results) = c("p.adjusted", "selected.genes")
    
    return(results)
    
  }
  
  #------------------------------------------------------------------------
  
  REP <- function(D,k)
  {
    r<-nrow(D)
    c<-ncol(D)
    DD<-NULL
    for(i in 1:c)
    {
      DDi <- rep(D[,i],k)
      DD <- cbind(DD,DDi)
    }
    colnames(DD)<-colnames(D)
    as.data.frame(DD)
  }
  
  #---------------------------------------------------------------------------
  
  Formula0 <- function(names,edesign=NULL)
  {
    formula <- "y~"
    
    if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
    else if(length(names)>1)
    {
      
      for (i in 1:(length(names)))
      {
        formula <- paste(formula,names[i],"+")
      }
      formula <- paste(formula, "transcript")
    }
    formula <- as.formula(formula)
    formula
  }
  
  #---------------------------------------------------------------------------
  
  Formula1 <- function(names)
  {
    formula <- "y~"
    
    if(length(names)==1){ formula=paste(formula,names[1],"* transcript") }
    else if(length(names)>1)
    {
      formula <- paste(formula,"(" )
      for (i in 1:(length(names)-1))
      {
        formula <- paste(formula,names[i],"+")
      }
      formula <- paste(formula,names[length(names)])
      formula <- paste(formula, ") * transcript")
    }
    formula <- as.formula(formula)
    formula
  }
  
  #---------------------------------------------------------------------------------------------------
  Formula000 <- function(names,edesign)
  {
    formula <- "y~"
    if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
    else if(length(names)>1)
    {
      name.time = colnames(edesign)[1]
      names.inter = paste(name.time,c("x","2x","3x","4x","5x"), sep="")
      COL.inter=NULL
      i=1
      col.i = grep(names.inter[i],names)
      COL.inter=col.i
      while(length(col.i)!=0)
      {
        i=i+1
        col.i = grep(names.inter[i],names)
        COL.inter=c(COL.inter,col.i)
      }
      names1=names[-COL.inter]
      names2=names[COL.inter]
      
      formula <- paste(formula,"(" )
      for (i in 1:(length(names1)-1))
      {
        formula <- paste(formula,names1[i],"+")
      }
      formula <- paste(formula,names1[length(names1)])
      formula <- paste(formula, ") * transcript")
    }
    
    formula <- paste(formula, "+")
    
    if(length(names2)>1){
      for (i in 1:(length(names2)-1))
      {formula <- paste(formula, names2[i],"+") }
    }
    formula <- paste(formula, names2[length(names2)])
    formula <- as.formula(formula)
    formula
  }
  
  #---------------------------------------------------------------
  
  
  Formula00 <- function(names,edesign)
  {
    formula <- "y~"
    if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
    else if(length(names)>1)
    {
      name.time = colnames(edesign)[1]
      names.conds = colnames(edesign)[3:ncol(edesign)]
      names.inter = paste(name.time,c("x","2x","3x","4x","5x"), sep="")
      COL.group = unique(unlist(sapply(names.conds, function(m) grep(m, names))))
      COL.inter = unique(unlist(sapply(names.inter, function(m) grep(m, names))))
      
      COL.out = unique(c(COL.group,COL.inter))
      
      names1=names[-COL.out]
      names2=names[COL.out]
      
      formula <- paste(formula,"(" )
      for (i in 1:(length(names1)-1))
      {
        formula <- paste(formula,names1[i],"+")
      }
      formula <- paste(formula,names1[length(names1)])
      formula <- paste(formula, ") * transcript")
    }
    
    formula <- paste(formula, "+")
    
    if(length(names2)>1){
      for (i in 1:(length(names2)-1))
      {formula <- paste(formula, names2[i],"+") }
    }
    formula <- paste(formula, names2[length(names2)])
    formula <- as.formula(formula)
    formula
  }

#-------------------------------------------------------
# PodiumChange_tappas
#-------------------------------------------------------

PodiumChange_tappas <- function(iso, only.sig.genes = FALSE, comparison=c("any","group","specific"), group.name="Ctr", time.points=0)
{
  gen <- iso$gen
  data <- iso$data
  edesign <- iso$design$edesign
  repvect = edesign[,2]

  if(only.sig.genes){
    data.clust<-as.matrix(data[gen%in%iso$DSG,])  
    sig.iso2<-rownames(data.clust)
    gen.sig.iso2 <- as.character(gen[rownames(data)%in%sig.iso2])
    # Here, there is not any mDSG because in this analysis it is considered only (>1 iso)
  }else{
    gen2 <- names(which(tapply(rownames(data), gen, length) >1))
    data.clust<-as.matrix(data[gen%in%gen2,])  
    sig.iso2<-rownames(data.clust)
    gen.sig.iso2 <- as.character(gen[rownames(data)%in%sig.iso2])
  }
  
#-------------------------------------------------------
# Major Isoform identification
#-------------------------------------------------------
  
  time.M <- tapply(edesign[,1],repvect,mean)
  groups.M <- apply(edesign[,3:ncol(edesign),drop=F],2,function(x){tapply(x,repvect,mean)})        ##### Added -> drop=F!!!!!
  
  unic <- unique(gen.sig.iso2)
  Mayor=NULL
  LIST = NULL
  TIMELIST = list()
  #if(comparison=="any"){for(group.name in colnames(groups.M)){TIMELIST[[group.name]] = c()}}
  for(i in 1:length(unic)) 
  {
    zz<-data.clust[gen.sig.iso2==unic[i],]
    M <- MayorIso(zz)
    zzM<-zz[M==1,]
    if(length(zzM)>length(repvect)){
      #print(zzM)
      zzM <- zzM[1,]
      MzzM <- tapply(zzM,repvect,mean)
      zzm <- zz[M!=max(M),]
      if(length(zzm)==0){
        zzm <- zz[nrow(zz),]
      }
    }else{
    MzzM <- tapply(zzM,repvect,mean)
    zzm <- zz[M!=max(M),]
    }
    
    if(is.null(nrow(zzm))) ni=1 else ni=nrow(zzm)
  
    if(ni==1) Mzzm=tapply(zzm,repvect,mean) else Mzzm <- t(apply(zzm, 1, function(x){tapply(x, repvect, mean)}))
    
    if(ni==1) dif=MzzM - Mzzm else dif <- t(apply(Mzzm, 1, function(x){MzzM-x}))
# Comparison = "a" ----------------------------------------------    
    if(comparison=="any"){  
      if( any(dif<0) ){ 
        LIST <- c( LIST, unic[i])
        if(ni==1) t.points=names(dif[dif<0]) else t.points=colnames(dif[,apply(dif,2,function(x) any(x<0)), drop=F])
          r = data.frame(groups.M[t.points,,drop=F], t.point=time.M[t.points])
        for (group.name in colnames(groups.M)){
          if(length(r[which(r[,group.name]==1),'t.point'])>0) {TIMELIST[[group.name]][unic[i]] = paste(r[which(r[,group.name]==1),'t.point'], collapse = ",")}
      }
      }
    }
      
# Comparison = "specific" ----------------------------------------------
      else if(comparison=="specific"){
      col <- groups.M[,colnames(groups.M)==group.name]
      if(ni==1) change <- all(dif[col==1 & time.M==time.points]<0)
      else change <- apply(dif[,col==1 & time.M==time.points,drop=F],1,function(x){all(x<0)})       ##### Added -> drop=F!!!!!
      if(any(change)) LIST <- c( LIST, unic[i]) } 
# Comparison = group ----------------------------------------------
  else if(comparison=="group"){
  	mayors = NULL
  	for (k in 3:ncol(edesign))
  	{
  	mayors = cbind(mayors, MayorIso(zz[,edesign[,k]==1]))
  	} 
  #When all columns match, substraction with any of them will be 0:
  	if (any(mayors-mayors[,1]!=0) )  LIST <- c( LIST, unic[i]) }                 #### any instead of all  !!!!
  }

# lists of genes and isoforms:
  gen.L <- gen.sig.iso2 [gen.sig.iso2 %in% LIST]
  data.L <- data.clust[gen.sig.iso2 %in% LIST,]
  
  output <- list(LIST, TIMELIST, data.L,  gen.L, edesign)
  names(output) <- c("L","T", "data.L", "gen.L","edesign")
  output
  }
    

#---------------------------------------------------------------------------------------------------
# Auxiliar internal functions: f3, MayorIso  
#---------------------------------------------------------------------------------------------------

f3 <- function(x)
{
  x<-x[!is.na(x)]
  y<-x[1]
  for (i in 2:length(x))
  {
    y<-paste(y,x[i],sep="&")
  }
  y
}
# ejemplo:
# x<-c(1,2,4,NA,NA)
# f3(x)


#-------------------------------------------------------

MayorIso<-function(zz)
{
  if( is.null(nrow(zz) )) 
  {
    sol=1 }
  else{
    M <-apply(zz,1,sum)
    sol=as.numeric(M==max(M))
  }
  sol
}

#
# END maSigPro custom changes - should be incorporated into R package later
#

#-------------------------------------------------------
# Favored_tappas
#-------------------------------------------------------

Favored_tappas <- function(exp, design)
{
  #data <- iso$data
  data <- exp
  edesign <- design$edesign
  repvect = edesign[,2]
  multiple = ncol(edesign) - 2 > 1
  toMatch = paste(c("_with$", "_without$"),collapse="|")
  genes = rownames(data)
  motif = rep(FALSE, nrow(data))
  motif[grep(pattern = "_with$",  x = genes)] <- TRUE
  genes = gsub(pattern = toMatch, replacement = "", x = genes)

  LIST = NULL
  if(multiple){
    geneUniq = unique(genes)
    for(i in 1:length(geneUniq)) {
      mayors = NULL
      zz<-data[genes==geneUniq[i],,drop=F]
      groups.M = colnames(edesign)[3:ncol(edesign)]
      for (k in 3:ncol(edesign))
      {
        mayors = cbind(mayors, MayorIso(zz[,edesign[,k]==1]))
      } 
      #When all columns match, substraction with any of them will be 0:
      if (any(mayors-mayors[,1]!=0) ){
        motifMayor = paste(groups.M[mayors[motif[genes==geneUniq[i]],]==1], collapse = ",")             
        LIST[[geneUniq[i]]] <- motifMayor
      }
  }
  }
  
  else{
    
    #zz<-data[genes==geneUniq[i],,drop=F]
    time.M <- sort(tapply(edesign[,1],repvect,mean))
    zzm <- t(apply(data, 1, function(x){tapply(x, repvect, mean)}))
    zzm = zzm[order(genes),names(time.M)]
    zzmMotif = zzm[which(motif),]
    zzmNMotif = zzm[!motif,]
    ## enter if just 1 vector for each one
    if(!is.matrix(zzmMotif)){
      n = list()
      zzmMotif = matrix(zzmMotif, nrow = 1, dimnames = list(rownames(zzm)[which(zzm==zzm[which(motif),])], colnames(zzm)))
      zzmNMotif = matrix(zzmNMotif, nrow = 1, dimnames = list(rownames(zzm)[which(zzm==zzm[which(!motif),])], colnames(zzm)))
      zzmMs = t(apply(zzmMotif, 1, function(x){sapply(2:length(zzmMotif), function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))
      zzmNMs = t(apply(zzmNMotif, 1, function(x){sapply(2:length(zzmNMotif), function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))
    }else{
      zzmMs = t(apply(zzmMotif, 1, function(x){sapply(2:ncol(zzmMotif),function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))
      zzmNMs = t(apply(zzmNMotif, 1, function(x){sapply(2:ncol(zzmNMotif),function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))
    }
    dif = zzmMs - zzmNMs
    rownames(dif) = unique(genes[order(genes)])
    
    LIST = apply(dif,1,function(x) paste(time.M[colnames(dif)[x>0]],collapse=","))
    LIST[LIST==""] <- time.M[1]
  
  }
  
  output <- list(LIST, edesign)
  names(output) <- c("F","edesign")
  output
  
}

#
# END Favored_tappas
#


#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential Feature Inclusion Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 14)
  stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""

restrictType = ""
cmpType = ""
degValue = ""

filteringType = ""
filterFC = ""
sig = ""
inFileCount = ""
outputTestFeatures = ""
outputTestGenes = ""
analysisId = ""
indirDFI = ""

for(i in 1:14) {
    if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-d", args[i])) > 0)
      indirDFI = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-u", args[i])) > 0)
      degValue = substring(args[i], 3)
    else if(length(grep("^-r", args[i])) > 0)
      restrictType = substring(args[i], 3)
    else if(length(grep("^-k", args[i])) > 0)
      cmpType = substring(args[i], 3)
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
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(indirDFI) == 0 || nchar(analysisId) == 0 || nchar(outputTestFeatures) == 0 || nchar(outputTestGenes) == 0 || nchar(inFileCount) == 0 || nchar(sig) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(method) == 0 || nchar(degValue) == 0 || nchar(restrictType) == 0 || nchar(cmpType) == 0 || nchar(filterFC) == 0 || nchar(filteringType) == 0)
  stop("Missing command line argument.")

#### determine what type of data to run DIU for
mff <- NULL
if(filterFC != "0") {
    mff = as.numeric(filterFC)
}
triple = FALSE
if(restrictType == "MORESTRICT")
    triple = TRUE
degree = as.numeric(degValue)
sig = as.numeric(sig)

#### Read input data and run analysis

# read expression factors definition table
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "time_factors.txt"), row.names=1, sep="\t", header=TRUE)
myfactors
groups = ncol(myfactors) - 2
times = length(unique(myfactors[,1]))
timepoints = 0
if(groups==1)
    timepoints = times
cat("\nGroups: ", groups, ", times: ", times)
design <- make.design.matrix(myfactors, degree)
minobs = (degree + 1) * groups

# read feature expression matrix normalized
cat("\nReading normalized feature matrix file data...")
featureMatrix = read.table(file.path(indirDFI, paste0("dfi_feature_matrix.",as.character(analysisId),".tsv")), row.names=1, sep="\t", quote=NULL, header=TRUE)
row.names(featureMatrix) <- lapply(row.names(featureMatrix), function(x) {gsub("GO:", "GO__", x)})
cat("\nRead ", nrow(featureMatrix), " normalized feature expression data rows")

# read gene feature map (gene-feature[-pos]-on/off to gene-feature[-pos]), where the position is optional
cat("\nReading gene features map...")
geneFeatures <- read.table(file.path(indirDFI, paste0("dfi_feature_id_map.",as.character(analysisId),".tsv")), sep="\t", quote=NULL, header=TRUE)
genes <- data.frame("feature"=geneFeatures$geneFeature, "id" = as.character(geneFeatures$featureId))
rownames(genes) <- genes[,1]
genes[,1] <- NULL
cat("\nRead ", nrow(genes), " feature id map data rows")
#head(genes)

# run DIU Analysis based on selected method - loop through all Sources and all Features within each source
result_dfi <- NULL
cont = 0
len_zero = F
sources <- ddply(geneFeatures, c("db"), summarise, N = length(result))
cat("Sources:\n")
print(head(sources))
for(db in sources$db) {
    gfDB <- geneFeatures[geneFeatures$db == db, ]
    dbCats <- ddply(gfDB, c("cat"), summarise, N = length(result))
    cat("SourceFeatures:\n")        
    print(head(dbCats$cat))
    for(category in dbCats$cat) {
        result_predfi <- NULL
        dbcatIds <- geneFeatures[geneFeatures$db == db & geneFeatures$cat == category, ]
        print(head(dbcatIds))
        dbcatVIds <- as.vector(dbcatIds[,c("geneFeature")])
        dbcatMatrix <- featureMatrix[dbcatVIds,]
        cat("DBCat matrix: ", nrow(dbcatMatrix), "\n")        
        print(head(dbcatMatrix))
        dbcatGenes <- data.frame("feature"=dbcatIds$geneFeature, "id" = as.character(dbcatIds$featureId))
        rownames(dbcatGenes) <- dbcatGenes[,1]
        dbcatGenes[,1] <- NULL
        cat("\nRead ", nrow(dbcatGenes), " feature id map data rows")
        #print(head(dbcatGenes))
        

        ### filter matrix
        cat("\nRead ", nrow(dbcatMatrix), " expression data rows")
        dbcatMatrix = dbcatMatrix[order(rownames(dbcatMatrix)),]
        if(!is.null(mff)){
            cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
            trans = minorFoldfilterTappas(dbcatMatrix, genes[rownames(dbcatMatrix),"id"], mff, minorMethod=filteringType)

            ### We need to remove the complementary isoforms (if delete without we also remove with)
            ### we look for isoforms we dont have to keep
            aux = setdiff(1:nrow(dbcatMatrix),which(rownames(dbcatMatrix) %in% trans))
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
              dbcatMatrix=dbcatMatrix[-res,]
            }
        }
      ### end filter
        if(nrow(dbcatMatrix) == 0){
          len_zero = T
          break
        } 
        # run analysis for all entries in this source:feature
        cont = cont+1
        results <- IsoModel_tappas(data=dbcatMatrix, gen=genes[rownames(dbcatMatrix),"id"], design=design, degree=degree,
                                  counts=TRUE, min.obs = minObs, triple=triple)#minorFoldfilter = mff, )
        print(names(results))
        cat("\nlenGen: ", length(results$gen), ", lenDSG: ", length(results$DSG), ", lenadjPV: ", length(results$adjpvalue))
        print(head(results$adjpvalue))

        # get the podium change information for these results
        pcList = PodiumChange_tappas(iso = results, only.sig.genes = FALSE, comparison = cmpType, time.points=timepoints)
        # add tested genes, default to podium False
        result_sig <- data.frame("gene"=names(results$adjpvalue), "adjPValue"=results$adjpvalue, "podiumChange"=rep(FALSE, times=length(results$adjpvalue)), "podiumTimes"=rep('.', times=length(results$adjpvalue)), "favoredTimes"=rep('.', times=length(results$adjpvalue)), stringsAsFactors=FALSE)
        print(paste("Number of tested genes: ", nrow(result_sig)))
        print(head(result_sig))
        # add Not tested genes, default to podium False
        list_notsig_genes = as.character(unique(dbcatIds$featureId[!(dbcatIds$featureId %in% result_sig$gene)]))
        if(!length(list_notsig_genes)==0){
          result_notsig <- data.frame("gene"=list_notsig_genes, "adjPValue"=1.0, "podiumChange"=FALSE, "podiumTimes"='.', "favoredTimes"='.', stringsAsFactors=FALSE)
          rownames(result_notsig) <- result_notsig$gene
          print(paste("Not tested(", nrow(result_notsig), "):"))
          print(head(result_notsig))
          result_predfi <- rbind(result_sig, result_notsig)
        }else{
          result_predfi <- result_sig
        }
        
        if(!length(pcList$L)==0){
          # set podium change flags
          result_predfi[result_predfi$gene %in% pcList$L, "podiumChange"] <- TRUE
  
          # set podium change time points if single series
          if(groups == 1) {
              # convert podium information from nested list to data frame
              print("Podium information:")
              #print(head(pcList))
              if(!length(pcList$T)==0){
                  podium_info <- data.frame("gene"=as.character(names(pcList$T[[1]])), "podiumTimes"=matrix(unlist(pcList$T[[1]]), nrow=length(pcList$T[[1]]), byrow=T), stringsAsFactors=FALSE)
                  print(head(podium_info))
                  # get index of rows to update in result_predfi table - order in match call is important
                  # we should never get an NA vector value in idx since all genes are in result_predfi
                  idx <- match(podium_info$gene, result_predfi$gene)
                  # if you need to remove at a later point use: idx <- idx[!is.na(idx)]
                  print(paste("Number of pdium gene matches:", length(idx)))
                  # update podium times
                  result_predfi[idx, "podiumTimes"] <- podium_info["podiumTimes"]
              }
              print(head(result_predfi))
          }
        }
        # set favored information - for both times series
        print("Favored information:")
        print(head(dbcatMatrix, 5))
        #print(head(design))
        fvList <- Favored_tappas(dbcatMatrix, design)
        if(!is.null(fvList$F)){
          print(head(fvList))
          print(names(fvList))
          # convert favored information from nested list to data frame
          favored_info <- data.frame("gene"=as.character(names(fvList$F)), "favoredTimes"=matrix(unlist(fvList$F), nrow=length(fvList$F), byrow=T), stringsAsFactors=FALSE)
          print(head(favored_info))
          # get index of rows to update in result_predfi table - order in match call is important
          # we should never get an NA vector value in idx since all genes are in result_predfi
          idx <- match(favored_info$gene, result_predfi$gene)
          # if you need to remove at a later point use: idx <- idx[!is.na(idx)]
          print(paste("Number of favored gene matches:", length(idx)))
          # update favored times
          result_predfi[idx, "favoredTimes"] <- favored_info["favoredTimes"]
        }
        print(head(result_predfi))

        result_dfi <- rbind(result_dfi, result_predfi)
    }
}

# if we dont have trans and we dont processed any feature before 
if(len_zero & cont == 0){
  cat("Final output\n")
  write.table(result_dfi, file.path(outdir, paste0("dfi_result.",as.character(analysisId),".tsv")), quote=FALSE, row.names=FALSE, sep="\t")
  # write completion file
  cat("You have 0 transcripts to analyze with this feature.\n")
}else{
  cat("Final output\n")
  head(result_dfi)
  
  # write results file
  write.table(result_dfi, file.path(outdir, paste0("dfi_result.",as.character(analysisId),".tsv")), quote=FALSE, row.names=FALSE, sep="\t")
  
  # write completion file
  cat("\nWriting DFInalysis completed file...")
  filedone <- file(file.path(outdir, paste0("done.",as.character(analysisId),".txt")))
  writeLines("done", filedone)
  close(filedone)

  

  cat("\nAll done.\n")
  
}