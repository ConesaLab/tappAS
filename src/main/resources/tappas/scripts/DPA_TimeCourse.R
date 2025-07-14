# Differential PolyAdenylation Analysis R script
#
# Differential Isoform Usage analysis will be performed using maSigPro 
#

#
# maSigPro custom changes - should be incorporated into R package later
#

library("maSigPro")
library("MASS")
library(plyr)
library("mclust")

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
  print (paste(nrow(data1), "remaining transcripts to analyse DPA"))   # changed
  print (paste(length(unique(Genes1)), "remaining genes to analyse DPA")) # changed
  
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
  toMatch = paste(c("_Long$", "_Short$"),collapse="|")
  genes = rownames(data)
  motif = rep(FALSE, nrow(data))
  motif[grep(pattern = "_Long$",  x = genes)] <- TRUE
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
    zzmMs = as.data.frame(apply(zzmMotif, 1, function(x){sapply(2:ncol(zzmMotif),function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))
    zzmNMs = as.data.frame(apply(zzmNMotif, 1, function(x){sapply(2:ncol(zzmNMotif),function(y) (x[y]-x[y-1])/(time.M[y]-time.M[y-1]))}))

    dif = zzmMs - zzmNMs
    rownames(dif) = unique(genes[order(genes)])
    colnames(dif) = c("dif")

    LIST = apply(dif,1,function(x) paste(ifelse(x>0,time.M[1],time.M[length(time.M)]),collapse=","))
    LIST[LIST==""] <- time.M[1]
    
  }
  
  output <- list(LIST, edesign)
  names(output) <- c("F","edesign")
  output
  
}

#
# END Favored_tappas
#

#
# Extra clusters funtions
#

# code generates an Rplots.pdf file as a result of opening a device w/o specifying a name - layout(), par(), etc. functions will do it
my.see.genes <- function (data, edesign = data$edesign, time.col = 1, repl.col = 2, 
                          group.cols = c(3:ncol(edesign)), names.groups = colnames(edesign)[3:ncol(edesign)], 
                          cluster.data = 1, groups.vector = data$groups.vector, k = 9, 
                          k.mclust = FALSE, cluster.method = "hclust", distance = "cor", 
                          agglo.method = "ward.D", show.fit = FALSE, dis = NULL, step.method = "backward", 
                          min.obs = 3, alfa = 0.05, nvar.correction = FALSE, show.lines = TRUE, 
                          iter.max = 500, summary.mode = "median", color.mode = "rainbow", 
                          cexlab = 1, legend = TRUE, newX11 = TRUE, ylim = NULL, main = NULL, filepath = NULL,
                          ...) 
{
  time = edesign[, time.col]
  repvect = edesign[, repl.col]
  groups = edesign[, group.cols]
  narrays <- length(time)
  if (!is.null(dim(data))) {
    dat <- as.data.frame(data)
    clusterdata <- data
  }
  else {
    clusterdata <- data[[cluster.data]]
    dat <- as.data.frame(data$sig.profiles)
  }
  if (nrow(dat) > 1) {
    dat <- as.data.frame(dat[, (ncol(dat) - length(time) + 
                                  1):ncol(dat)])
    count.noNa <- function(x) (length(x) - length(x[is.na(x)]))
    dat <- dat[which(apply(as.matrix(dat), 1, count.noNa) >= 
                       length(unique(repvect))), ]
    clusterdata <- dat
    if (any(is.na(clusterdata))) {
      if (cluster.method == "kmeans" || cluster.method == 
          "Mclust") {
        if (all(cluster.data != 1, cluster.data != "sig.profiles")) {
          clusterdata[is.na(clusterdata)] <- 0
        }
        else {
          mean.replic <- function(x) {
            tapply(as.numeric(x), repvect, mean, na.rm = TRUE)
          }
          MR <- t(apply(clusterdata, 1, mean.replic))
          if (any(is.na(MR))) {
            row.mean <- t(apply(MR, 1, mean, na.rm = TRUE))
            MRR <- matrix(row.mean, nrow(MR), ncol(MR))
            MR[is.na(MR)] <- MRR[is.na(MR)]
          }
          data.noNA <- matrix(NA, nrow(clusterdata), 
                              ncol(clusterdata))
          u.repvect <- unique(repvect)
          for (i in 1:nrow(clusterdata)) {
            for (j in 1:length(u.repvect)) {
              data.noNA[i, repvect == u.repvect[j]] = MR[i, 
                                                         u.repvect[j]]
            }
          }
          clusterdata <- data.noNA
        }
      }
    }
    if (!is.null(clusterdata)) {
      k <- min(k, nrow(dat), na.rm = TRUE)
      if (cluster.method == "hclust") {
        if (distance == "cor") {
          dcorrel <- matrix(rep(1, nrow(clusterdata)^2), 
                            nrow(clusterdata), nrow(clusterdata)) - cor(t(clusterdata), 
                                                                        use = "pairwise.complete.obs")
          clust <- hclust(as.dist(dcorrel), method = agglo.method)
          c.algo.used = paste(cluster.method, "cor", 
                              agglo.method, sep = "_")
        }
        else {
          clust <- hclust(dist(clusterdata, method = distance), 
                          method = agglo.method)
          c.algo.used = paste(cluster.method, distance, 
                              agglo.method, sep = "_")
        }
        cut <- cutree(clust, k = k)
      }
      else if (cluster.method == "kmeans") {
        cut <- kmeans(clusterdata, k, iter.max)$cluster
        c.algo.used = paste("kmeans", k, iter.max, sep = "_")
      }
      else if (cluster.method == "Mclust") {
        if (k.mclust) {
          print(paste0("calling Mclust with maxG: ", k))
          my.mclust <- Mclust(clusterdata, G=k)
          k = my.mclust$G
        }
        else {
          my.mclust <- Mclust(clusterdata, k)
        }
        cut <- my.mclust$class
        c.algo.used = paste("Mclust", k, sep = "_")
      }
      else stop("Invalid cluster algorithm")
      if (newX11) 
        X11()
      groups <- as.matrix(groups)
      colnames(groups) <- names.groups
      if (k <= 4) 
        par(mfrow = c(2, 2))
      else if (k <= 6) 
        par(mfrow = c(3, 2))
      else if (k > 6) 
        par(mfrow = c(3, 3))
      for (i in 1:(k)) {
        #PlotProfiles(data = dat[cut == i, ], repvect = repvect, 
        #  main = i, ylim = ylim, color.mode = color.mode, 
        #  cond = rownames(edesign), ...)
      }
      if (newX11) 
        X11()
      if (k <= 4) {
        par(mfrow = c(2, 2))
        cexlab = 0.6
      }
      else if (k <= 6) {
        par(mfrow = c(3, 2))
        cexlab = 0.6
      }
      else if (k > 6) {
        par(mfrow = c(3, 3))
        cexlab = 0.35
      }
      for (j in 1:(k)) {
        filename = NULL
        if (!is.null(filepath))
          filename = paste0(filepath, ".", j, ".png")
        print(paste0("a k: ", k, ", ylim: ", ylim))
        my.PlotGroups(data = dat[cut == j, ], show.fit = show.fit, 
                      dis = dis, step.method = step.method, min.obs = min.obs, 
                      alfa = alfa, nvar.correction = nvar.correction, 
                      show.lines = show.lines, time = time, groups = groups, 
                      repvect = repvect, summary.mode = summary.mode, 
                      xlab = "time", main = paste("Cluster", j, sep = " "), 
                      ylim = ylim, cexlab = 1.5, legend = legend, filepath = filename,
                      groups.vector = groups.vector, ...)
      }
    }
    else {
      print("warning: impossible to compute hierarchical clustering")
      c.algo.used <- NULL
      cut <- 1
    }
  }
  else if (nrow(dat) == 1) {
    if (newX11) 
      X11()
    #PlotProfiles(data = dat, repvect = repvect, main = NULL, 
    #    ylim = ylim, color.mode = color.mode, cond = rownames(edesign), 
    #    ...)
    if (newX11) 
      X11()
    filename = NULL
    if (!is.null(filepath))
      filename = paste0(filepath, ".", 1, ".png")
    print(paste0("b k: ", k, ", ylim: ", ylim))
    my.PlotGroups(data = dat, show.fit = show.fit, dis = dis, 
                  step.method = step.method, min.obs = min.obs, alfa = alfa, 
                  nvar.correction = nvar.correction, show.lines = show.lines, 
                  time = time, groups = groups, repvect = repvect, 
                  summary.mode = summary.mode, xlab = "time", main = main, 
                  ylim = ylim, cexlab = cexlab, legend = legend, filepath = filename, groups.vector = groups.vector, 
                  ...)
    c.algo.used <- NULL
    cut <- 1
  }
  else {
    print("warning: NULL data. No visualization possible")
    c.algo.used <- NULL
    cut <- NULL
  }
  OUTPUT <- list(cut, c.algo.used, groups)
  names(OUTPUT) <- c("cut", "cluster.algorithm.used", "groups")
  OUTPUT
}

my.PlotGroups <- function (data, edesign = NULL, time = edesign[, 1], groups = edesign[, 
                                                                                       c(3:ncol(edesign))], repvect = edesign[, 2], show.fit = FALSE, 
                           dis = NULL, step.method = "backward", min.obs = 2, alfa = 0.05, 
                           nvar.correction = FALSE, summary.mode = "median", show.lines = TRUE, 
                           groups.vector = NULL, xlab = "Time", ylab = "Differential PolyA Usage", filepath = "cluster",
                           cex.xaxis = 1.5, ylim = NULL, main = NULL, cexlab = 1.5, legend = TRUE, 
                           sub = NULL) 
{
  if (!is.vector(data)) {
    if (summary.mode == "representative") {
      distances <- apply(as.matrix(dist(data, diag = TRUE, 
                                        upper = TRUE)), 1, sum)
      representative <- names(distances)[distances == min(distances)]
      yy <- as.numeric(data[rownames(data) == representative, 
                            ])
      sub <- paste("Representative:", representative)
    }
    else if (summary.mode == "median") {
      yy <- apply(as.matrix(data), 2, median, na.rm = TRUE)
      sub <- paste("Median profile of ", nrow(data), " genes")
    }
    else stop("not valid summary.mode")
    if (dim(data)[1] == 1) {
      sub <- rownames(data)
    }
  }
  else if (length(data) != 0) {
    yy <- as.numeric(data)
    sub <- rownames(data)
  }
  else stop("empty data")
  if (is.null(ncol(groups))) {
    ncol = 1
    legend = FALSE
    codeg = "group"
  }
  else {
    ncol = ncol(groups)
    codeg <- as.character(colnames(groups))
  }
  reps <- i.rank(repvect)
  y <- vector(mode = "numeric", length = length(unique(reps)))
  x <- vector(mode = "numeric", length = length(unique(reps)))
  g <- matrix(nrow = length(unique(reps)), ncol = ncol)
  for (k in 1:length(y)) {
    y[k] <- mean(yy[reps == k], na.rm = TRUE)
    x[k] <- mean(time[reps == k])
    for (j in 1:ncol) {
      g[k, j] <- mean(groups[reps == k, j])
    }
  }
  print("YY:")
  print(head(yy))
  if (is.null(ylim)) {
    ylim = c(min(as.numeric(yy), na.rm = TRUE), max(as.numeric(yy), na.rm = TRUE))
    print(paste0("ylim: ", ylim))
  }
  abcissa <- x
  xlim = c(min(abcissa, na.rm = TRUE), max(abcissa, na.rm = TRUE) * 
             1.3)
  color1 <- as.numeric(sort(factor(colnames(groups)))) + 1
  color2 <- groups
  for (j in 1:ncol) {
    color2[, j] <- color2[, j] * j
  }
  color2 <- as.vector(apply(color2, 1, sum) + 1)
  png(filename=filepath, width = 1200, height = 400, units = "px")
  plot(x = time, y = yy, pch = 21, xlab = xlab, ylab = ylab, 
       xaxt = "n", main = paste0(main, " - ", sub), ylim = ylim, xlim = xlim, 
       cex = cexlab, cex.main=1.5, cex.axis=1.5, cex.lab=1.5, cex.sub=1.5, col = color2)
  axis(1, at = unique(abcissa), labels = unique(abcissa), cex.axis = cex.xaxis)
  if (show.fit) {
    rm <- matrix(yy, nrow = 1, ncol = length(yy))
    rownames(rm) <- c("ratio medio")
    colnames(rm) <- rownames(dis)
    fit.y <- T.fit(rm, design = dis, step.method = step.method, 
                   min.obs = min.obs, alfa = alfa, nvar.correction = nvar.correction)
    betas <- fit.y$coefficients
  }
  for (i in 1:ncol(groups)) {
    group <- g[, i]
    if ((show.fit) && !is.null(betas)) {
      li <- c(2:6)
      a <- reg.coeffs(coefficients = betas, groups.vector = groups.vector, 
                      group = colnames(groups)[i])
      a <- c(a, rep(0, (7 - length(a))))
      curve(a[1] + a[2] * x + a[3] * (x^2) + a[4] * (x^3) + 
              a[5] * (x^4) + a[6] * (x^5) + a[7] * (x^5), from = min(time), 
            to = max(time), col = color1[i], add = TRUE, 
            lty = li[i])
    }
    if (show.lines) {
      lx <- abcissa[group != 0]
      ly <- y[group != 0]
      ord <- order(lx)
      lxo <- lx[ord]
      lyo <- ly[ord]
      lines(lxo, lyo, col = color1[i])
    }
  }
  op <- par(bg = "white")
  if (legend) 
    legend(max(abcissa, na.rm = TRUE) * 1.02, ylim[1], legend = codeg, 
           text.col = color1, col = color1, cex = 1.5, lty = 1, 
           yjust = 0)
  par(op)
  dev.off()
}

# code generates an Rplots.pdf file as a result of opening a device w/o specifying a name - layout(), par(), etc. functions will do it
my.see.genes <- function (data, edesign = data$edesign, time.col = 1, repl.col = 2, 
                          group.cols = c(3:ncol(edesign)), names.groups = colnames(edesign)[3:ncol(edesign)], 
                          cluster.data = 1, groups.vector = data$groups.vector, k = 9, 
                          k.mclust = FALSE, cluster.method = "hclust", distance = "cor", 
                          agglo.method = "ward.D", show.fit = FALSE, dis = NULL, step.method = "backward", 
                          min.obs = 3, alfa = 0.05, nvar.correction = FALSE, show.lines = TRUE, 
                          iter.max = 500, summary.mode = "median", color.mode = "rainbow", 
                          cexlab = 1, legend = TRUE, newX11 = TRUE, ylim = NULL, main = NULL, filepath = NULL,
                          ...) 
{
  time = edesign[, time.col]
  repvect = edesign[, repl.col]
  groups = edesign[, group.cols]
  narrays <- length(time)
  if (!is.null(dim(data))) {
    dat <- as.data.frame(data)
    clusterdata <- data
  }
  else {
    clusterdata <- data[[cluster.data]]
    dat <- as.data.frame(data$sig.profiles)
  }
  if (nrow(dat) > 1) {
    dat <- as.data.frame(dat[, (ncol(dat) - length(time) + 
                                  1):ncol(dat)])
    count.noNa <- function(x) (length(x) - length(x[is.na(x)]))
    dat <- dat[which(apply(as.matrix(dat), 1, count.noNa) >= 
                       length(unique(repvect))), ]
    clusterdata <- dat
    if (any(is.na(clusterdata))) {
      if (cluster.method == "kmeans" || cluster.method == 
          "Mclust") {
        if (all(cluster.data != 1, cluster.data != "sig.profiles")) {
          clusterdata[is.na(clusterdata)] <- 0
        }
        else {
          mean.replic <- function(x) {
            tapply(as.numeric(x), repvect, mean, na.rm = TRUE)
          }
          MR <- t(apply(clusterdata, 1, mean.replic))
          if (any(is.na(MR))) {
            row.mean <- t(apply(MR, 1, mean, na.rm = TRUE))
            MRR <- matrix(row.mean, nrow(MR), ncol(MR))
            MR[is.na(MR)] <- MRR[is.na(MR)]
          }
          data.noNA <- matrix(NA, nrow(clusterdata), 
                              ncol(clusterdata))
          u.repvect <- unique(repvect)
          for (i in 1:nrow(clusterdata)) {
            for (j in 1:length(u.repvect)) {
              data.noNA[i, repvect == u.repvect[j]] = MR[i, 
                                                         u.repvect[j]]
            }
          }
          clusterdata <- data.noNA
        }
      }
    }
    if (!is.null(clusterdata)) {
      k <- min(k, nrow(dat), na.rm = TRUE)
      if (cluster.method == "hclust") {
        if (distance == "cor") {
          dcorrel <- matrix(rep(1, nrow(clusterdata)^2), 
                            nrow(clusterdata), nrow(clusterdata)) - cor(t(clusterdata), 
                                                                        use = "pairwise.complete.obs")
          clust <- hclust(as.dist(dcorrel), method = agglo.method)
          c.algo.used = paste(cluster.method, "cor", 
                              agglo.method, sep = "_")
        }
        else {
          clust <- hclust(dist(clusterdata, method = distance), 
                          method = agglo.method)
          c.algo.used = paste(cluster.method, distance, 
                              agglo.method, sep = "_")
        }
        cut <- cutree(clust, k = k)
      }
      else if (cluster.method == "kmeans") {
        cut <- kmeans(clusterdata, k, iter.max)$cluster
        c.algo.used = paste("kmeans", k, iter.max, sep = "_")
      }
      else if (cluster.method == "Mclust") {
        if (k.mclust) {
          print(paste0("calling Mclust with maxG: ", k))
          my.mclust <- Mclust(clusterdata, G=k)
          k = my.mclust$G
        }
        else {
          my.mclust <- Mclust(clusterdata, k)
        }
        cut <- my.mclust$class
        c.algo.used = paste("Mclust", k, sep = "_")
      }
      else stop("Invalid cluster algorithm")
      if (newX11) 
        X11()
      groups <- as.matrix(groups)
      colnames(groups) <- names.groups
      if (k <= 4) 
        par(mfrow = c(2, 2))
      else if (k <= 6) 
        par(mfrow = c(3, 2))
      else if (k > 6) 
        par(mfrow = c(3, 3))
      for (i in 1:(k)) {
        #PlotProfiles(data = dat[cut == i, ], repvect = repvect, 
        #  main = i, ylim = ylim, color.mode = color.mode, 
        #  cond = rownames(edesign), ...)
      }
      if (newX11) 
        X11()
      if (k <= 4) {
        par(mfrow = c(2, 2))
        cexlab = 0.6
      }
      else if (k <= 6) {
        par(mfrow = c(3, 2))
        cexlab = 0.6
      }
      else if (k > 6) {
        par(mfrow = c(3, 3))
        cexlab = 0.35
      }
      for (j in 1:(k)) {
        filename = NULL
        if (!is.null(filepath))
          filename = paste0(filepath, ".", j, ".png")
        print(paste0("a k: ", k, ", ylim: ", ylim))
        my.PlotGroups(data = dat[cut == j, ], show.fit = show.fit, 
                      dis = dis, step.method = step.method, min.obs = min.obs, 
                      alfa = alfa, nvar.correction = nvar.correction, 
                      show.lines = show.lines, time = time, groups = groups, 
                      repvect = repvect, summary.mode = summary.mode, 
                      xlab = "time", main = paste("Cluster", j, sep = " "), 
                      ylim = ylim, cexlab = 1.5, legend = legend, filepath = filename,
                      groups.vector = groups.vector, ...)
      }
    }
    else {
      print("warning: impossible to compute hierarchical clustering")
      c.algo.used <- NULL
      cut <- 1
    }
  }
  else if (nrow(dat) == 1) {
    if (newX11) 
      X11()
    #PlotProfiles(data = dat, repvect = repvect, main = NULL, 
    #    ylim = ylim, color.mode = color.mode, cond = rownames(edesign), 
    #    ...)
    if (newX11) 
      X11()
    filename = NULL
    if (!is.null(filepath))
      filename = paste0(filepath, ".", 1, ".png")
    print(paste0("b k: ", k, ", ylim: ", ylim))
    my.PlotGroups(data = dat, show.fit = show.fit, dis = dis, 
                  step.method = step.method, min.obs = min.obs, alfa = alfa, 
                  nvar.correction = nvar.correction, show.lines = show.lines, 
                  time = time, groups = groups, repvect = repvect, 
                  summary.mode = summary.mode, xlab = "time", main = main, 
                  ylim = ylim, cexlab = cexlab, legend = legend, filepath = filename, groups.vector = groups.vector, 
                  ...)
    c.algo.used <- NULL
    cut <- 1
  }
  else {
    print("warning: NULL data. No visualization possible")
    c.algo.used <- NULL
    cut <- NULL
  }
  OUTPUT <- list(cut, c.algo.used, groups)
  names(OUTPUT) <- c("cut", "cluster.algorithm.used", "groups")
  OUTPUT
}

#
# End clusters functions
#

#### validate command line arguments

args = commandArgs(TRUE)
cat("Differential PolyAdenylation Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 12)
  stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""
restrictType = ""
cmpType = ""
degValue = ""
filterFC = ""
kvalue = ""
mvalue = ""
minLength = ""
filteringType = ""
groupNames = ""
for(i in 1:12) {
    if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else if(length(grep("^-u", args[i])) > 0)
      degValue = substring(args[i], 3)
    else if(length(grep("^-r", args[i])) > 0)
      restrictType = substring(args[i], 3)
    else if(length(grep("^-g", args[i])) > 0)
      cmpType = substring(args[i], 3)
    else if(length(grep("^-f", args[i])) > 0)
      filterFC = substring(args[i], 3)
    else if(length(grep("^-k", args[i])) > 0)
      kvalue = substring(args[i], 3)
    else if(length(grep("^-l", args[i])) > 0)
      minLength = substring(args[i], 3)
    else if(length(grep("^-c", args[i])) > 0)
      mvalue = substring(args[i], 3)
    else if(length(grep("^-t", args[i])) > 0)
      filteringType = substring(args[i], 3)
    else if(length(grep("^-n", args[i])) > 0)
      groupNames = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(groupNames) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(method) == 0 || nchar(minLength) == 0 || nchar(degValue) == 0 || nchar(restrictType) == 0 || nchar(cmpType) == 0 || nchar(filterFC) == 0 || nchar(kvalue) == 0 || nchar(mvalue) == 0 || nchar(filteringType) == 0)
  stop("Missing command line argument.")

#####
# TEST VALUES
#####

#indir = "."
#utrdir = "."
#outdir = "."
#minLength = "100"
#filteringType = "PROP"
#mff = 0.1
#groupNames = c("NONOkoCT6", "NONOkoCT18")
#degree = 1

#### determine what type of data to run DPA for

minLength = as.numeric(minLength)

mff <- NULL
if(filterFC != "0") {
  mff = as.numeric(filterFC)
}
triple = FALSE
if(restrictType == "MORESTRICT")
  triple = TRUE
degree = as.numeric(degValue)

#### determine k values
knum = as.numeric(kvalue)
usemclust = TRUE
if(mvalue == "0")
  usemclust = FALSE

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

transMatrix = transMatrix[order(rownames(transMatrix)),]
cat("\nIntersecting DPA information with transcript matrix...")
for(i in (rownames(transMatrix))){
  if(i %in% infoDPA$Trans){
    genes_trans = c(genes_trans, infoDPA$Gene[which(infoDPA$Trans==i)])
    index = c(index, which(rownames(transMatrix)==i))
  }
}

transMatrix = transMatrix[index,]

# #Filtering by FOLD/PROP [code to mantain not tested genes]
# transMatrix_original = transMatrix
# genes_trans_original = genes_trans
# 
# if(!is.null(mff)){
#   cat(paste0("\nFiltering new transcript matrix by ",filteringType,"...\n"))
#   trans = minorFoldfilterTappas(transMatrix, genes_trans, mff, minorMethod=filteringType)
#   transMatrix=transMatrix[trans,]
# }

#Filtering by FOLD/PROP all isoforms
if(!is.null(mff)){
  cat(paste0("\nFiltering transcript matrix by ",filteringType,"...\n"))
  trans = minorFoldfilterTappas(transMatrix, genes_trans, mff, minorMethod=filteringType)
  transMatrix=transMatrix[trans,]
}

#infoDPA with same transMatrix length
#infoDPA_original = infoDPA[which(infoDPA$Trans %in% rownames(transMatrix_original)),c(1,2,3)]
infoDPA = infoDPA[which(infoDPA$Trans %in% rownames(transMatrix)),c(1,2,3)]

#delete genes with 1 trans after filtering
for(gene in unique(infoDPA$Gene)){
  if(length(infoDPA[infoDPA$Gene==gene,"Gene"])==1)
    infoDPA=infoDPA[-which(infoDPA$Gene==gene),]
}

#Create L and S category [gene/trans/PAS/length_to_compare] #already checked if GenPos > length passed by user
cat("\nCreating categories for Distal and Proximal transcripts...")
df.infoDPA = data.frame(Gene = infoDPA$Gene, Trans = infoDPA$Trans, Pos = infoDPA$GenPos, Cat = "", stringsAsFactors = F)
for(gene in sort(unique(df.infoDPA$Gene))){
  maxPos = max(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
  minPos = min(df.infoDPA[df.infoDPA$Gene==gene,]$Pos)
  #just count genes with abs(maxPos-minPos>minLength)
  if(abs(maxPos-minPos)>minLength){
    for(trans in df.infoDPA[df.infoDPA$Gene==gene,]$Trans){
      transPos = df.infoDPA[df.infoDPA$Trans==trans,]$Pos
      df.infoDPA[df.infoDPA$Trans==trans,]$Cat = ifelse(abs(transPos-minPos)<abs(transPos-maxPos),"S","L")
    }
  }
}

# #Same to the original
# df.infoDPA_original = data.frame(Gene = infoDPA_original$Gene, Trans = infoDPA_original$Trans, Pos = infoDPA_original$GenPos, Cat = "", stringsAsFactors = F)
# for(gene in sort(unique(df.infoDPA_original$Gene))){
#   maxPos = max(df.infoDPA_original[df.infoDPA_original$Gene==gene,]$Pos)
#   minPos = min(df.infoDPA_original[df.infoDPA_original$Gene==gene,]$Pos)
#   #just count genes with abs(maxPos-minPos>minLength)
#   if(abs(maxPos-minPos)>minLength){
#     for(trans in df.infoDPA_original[df.infoDPA_original$Gene==gene,]$Trans){
#       transPos = df.infoDPA_original[df.infoDPA_original$Trans==trans,]$Pos
#       df.infoDPA_original[df.infoDPA_original$Trans==trans,]$Cat = ifelse(abs(transPos-minPos)<abs(transPos-maxPos),"S","L")
#     }
#   }
# }

#take only if has category
df.infoDPA = df.infoDPA[-which(df.infoDPA$Cat==""),]
# df.infoDPA_original = df.infoDPA_original[-which(df.infoDPA_original$Cat==""),]

df.infoDPA = df.infoDPA[order(df.infoDPA$Gene),]
# df.infoDPA_original = df.infoDPA_original[order(df.infoDPA_original$Gene),]
write.table(df.infoDPA,file.path(outdir, "dpa_cat.tsv"), quote=FALSE, row.names=FALSE, sep="\t")
# write.table(df.infoDPA_original,file.path(outdir, "dpa_cat_original.tsv"), quote=FALSE, row.names=FALSE, sep="\t")

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
# df.infoDPA_original <- df.infoDPA_original[order(df.infoDPA_original$Cat),]

#infoDPA
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

# #infoDPA_original
# names_L <- NULL
# names_S <- NULL
# trans_exprs_original <- NULL
# names_genes_original <- NULL
# trans_exprs_L_original <- matrix(, nrow = 0, ncol = ncol(transMatrix_original))
# trans_exprs_S_original <- matrix(, nrow = 0, ncol = ncol(transMatrix_original))
# 
# for(gene in sort(unique(df.infoDPA_original$Gene))) {
#   L_trans <- df.infoDPA_original[which(df.infoDPA_original$Gene==gene & df.infoDPA_original$Cat=="L"),]$Trans
#   S_trans <- df.infoDPA_original[which(df.infoDPA_original$Gene==gene & df.infoDPA_original$Cat=="S"),]$Trans
#   trans_exprs_L_original <- rbind(trans_exprs_L_original,apply(transMatrix_original[L_trans,],2,sum))
#   trans_exprs_S_original <- rbind(trans_exprs_S_original,apply(transMatrix_original[S_trans,],2,sum))
#   names_L <- c(names_L,paste0(gene,"_Long"))
#   names_S <- c(names_S,paste0(gene,"_Short"))
#   names_genes_original <- c(names_genes,gene,gene)
# }
# 
# rownames(trans_exprs_L_original) <- names_L
# rownames(trans_exprs_S_original) <- names_S
# trans_exprs_original <- rbind(trans_exprs_L_original, trans_exprs_S_original[, colnames(trans_exprs_L_original)])
# trans_exprs_original <- trans_exprs_original[order(rownames(trans_exprs_original)),]

# run analysis
results <- IsoModel_tappas(data=data.frame(trans_exprs), gen=names_genes, design=design, degree=degree,
                           counts=TRUE, min.obs = minObs, triple=triple) #minorFoldfilter = mff, 

head(results$adjpvalue)

############
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
if(!is.null(mff)){
  #result_notsig <- data.frame("gene"=as.character(infoDPA_original$Gene[!(infoDPA_original$Gene %in% result_sig$gene)]), "adjPValue"=1.0, "podiumChange"=FALSE, "podiumTimes"='.', "favoredTimes"='.', stringsAsFactors=FALSE)
}else{
  result_notsig <- result_sig[which(result_sig$adjPValue==1),]
}
#print(paste("Not tested(", nrow(result_notsig), "):"))
#print(head(result_notsig))
result_predpa <- rbind(result_sig)#, result_notsig)

# set podium change flags
result_predpa[result_predpa$gene %in% pcList$L, "podiumChange"] <- TRUE
# set podium change time points if single series
if(groups == 1) {
  # convert podium information from nested list to data frame
  print("Podium information:")
  #print(head(pcList))
  if(!length(pcList$T)==0){
    podium_info <- data.frame("gene"=as.character(names(pcList$T[[1]])), "podiumTimes"=matrix(unlist(pcList$T[[1]]), nrow=length(pcList$T[[1]]), byrow=T), stringsAsFactors=FALSE)
    print(head(podium_info))
    # get index of rows to update in result_predpa table - order in match call is important
    # we should never get an NA vector value in idx since all genes are in result_predpa
    idx <- match(podium_info$gene, result_predpa$gene)
    # if you need to remove at a later point use: idx <- idx[!is.na(idx)]
    print(paste("Number of podium gene matches:", length(idx)))
    # update podium times
    result_predpa[idx, "podiumTimes"] <- podium_info["podiumTimes"]
  }
  print(head(result_predpa))
}

# set favored information - for both times series
print("Favored information:")
print(head(data.frame(trans_exprs)))
#print(head(design))
fvList <- Favored_tappas(data.frame(trans_exprs), design)
#print(head(fvList))
#print(names(fvList))
# convert favored information from nested list to data frame
print("Favored information2:")
favored_info <- data.frame("gene"=as.character(names(fvList$F)), "favoredTimes"=matrix(unlist(fvList$F), nrow=length(fvList$F), byrow=T), stringsAsFactors=FALSE)
print(head(favored_info),5)
# get index of rows to update in result_predpa table - order in match call is important
# we should never get an NA vector value in idx since all genes are in result_predpa
idx <- match(favored_info$gene, result_predpa$gene)
# if you need to remove at a later point use: idx <- idx[!is.na(idx)]
print(paste("Number of favored gene matches:", length(idx)))
# update favored times
result_predpa[idx, "favoredTimes"] <- favored_info["favoredTimes"]
print(head(result_predpa))

result_dpa <- rbind(result_dpa, result_predpa)
############

cat("Final output\n")
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
a = a[order(substring(rownames(a),1,nchar(rownames(a))-nchar("_Long"))),]
write.table(a, file.path(indir, "diff_PolyA_matrix_mean.tsv"), quote=FALSE, row.names=TRUE,col.names = FALSE, sep="\t")

# a <- data.frame(row.names = rownames(trans_exprs_original))
# for(i in 1:(times*groups)){
#   a[i] <- apply(trans_exprs_original[,(i*2-1):(i*2)],1,mean)
# }
# a = a[order(substring(rownames(a),1,nchar(rownames(a))-nchar("_Long"))),]
# write.table(a, file.path(indir, "diff_PolyA_matrix_mean_original.tsv"), quote=FALSE, row.names=TRUE,col.names = FALSE, sep="\t")

###########
# Plots
###########

## Only significant genes
ind = result_dpa[which(result_dpa[,"adjPValue"]<=0.05),"gene"]

#Building L/S expresion matrix
#b <- matrix(nrow = nrow(a)/2, ncol = ncol(a))
names <- NULL
for(i in seq(1,nrow(a),2)){
  names <- c(names, substring(rownames(a[i,]),1,nchar(rownames(a[i,]))-nchar("_Long")))
}
b <- matrix(nrow = length(names), ncol = ncol(a))
for(i in 1:nrow(b)){
  b[i,] <- as.matrix(a[i*2-1,] / a[i*2,])
}
b = as.matrix(b)
rownames(b) <- names
colnames(b) <- colnames(a)
b = b[which(rownames(b) %in% ind),]

########## Check this 
factors <- data.frame("Time" = unique(myfactors$Time), "Replicate" = unique(myfactors$Replicate))
rownames(factors) <- colnames(a)

## depende de si single, multi
for(i in 1:(ncol(myfactors)-2)){
  factors[,colnames(myfactors[i+2])] = myfactors[seq(1,nrow(factors)*2,2),i+2]  
}
design <- make.design.matrix(factors, degree)

# generate cluster plots by groups
for(i in 1:(ncol(design$edesign)-2)) {
  gname <- names(design$edesign[i+2])
  cat("\nGenerating cluster graphs for ", gname, "...\n")
  filepath=file.path(outdir, paste0("cluster_", gname))
  cmethod = "hclust"
  if(isTRUE(usemclust))
    cmethod = "Mclust"
  cat("\nclusterMethod: ", cmethod, ", useMclust: ", usemclust)
  num_col = c(1,2,which(colnames(design$edesign)==gname))  
  #num_col_dis = c(1,seq(1+i,ncol(design$dis-1),groups))
  num_col_dis = NULL
  clusters <- my.see.genes(b[,as.logical(design$edesign[,gname])], edesign=design$edesign[as.logical(design$edesign[,gname]),num_col], show.fit = F, dis=design$dis[as.logical(design$edesign[,gname]),num_col_dis], cluster.method=cmethod, cluster.data = 1, k=knum, k.mclust=usemclust, min.obs = 0.001, newX11 = FALSE, filepath = filepath)
  if(!is.null(clusters)) {
    if(!is.null(clusters$cluster.algorithm.used)) {
      # create a data frame with cluster information and save
      dfc <- data.frame(clusters$cut)
      head(dfc)
      cat("\nSaving DPA clusters data to file...")
      write.table(dfc, file.path(outdir, paste0("cluster_", gname, ".tsv")), quote=FALSE, row.names=TRUE, sep="\t")
    } else {
      cat("\nWARNING: Unable to generate clusters (something is.null).\n")
    }   
  } else {
    cat("\nWARNING: Unable to generate clusters.\n")
  }
}

# write completion file
cat("\nWriting DPAnalysis completed file...")
filedone <- file(file.path(outdir, "done.txt"))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")