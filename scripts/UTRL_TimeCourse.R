# UTR LEngthening Analysis R script
#
# UTR Lengthening analysis will be performed by using two different methods:
#   WILCOXON
#   masigpro??
#

library(plyr)
library(ggpubr)
library(tidyr)
library(dplyr)

library("maSigPro")
library("MASS")
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
#
# IsoModel_tappas <- function(data, gen, design = NULL, degree = 2, Q = 0.05, min.obs = 6, minorFoldfilter = NULL,
#                             counts = FALSE, family = NULL, theta = 10, epsilon = 1e-05, triple = FALSE)
# {
#   #---------------------------------------------------------------------------------------------------
#   # data is a matrix containing isoform expression. Isoforms must be in rows and experimental conditions in columns
#   # gen is a vector with the name of the gene each isoform belongs to
#   #---------------------------------------------------------------------------------------------------
#
#   Genes <- unique(gen)
#   g <- length(Genes)
#
#   if (is.null(family)) {
#     if (!counts) {
#       family = gaussian()
#     }
#     if (counts) {
#       family = negative.binomial(theta)
#     }
#   }
#
#   #---------------------------------------------------------------------------------------------------
#   # STEP -1: Remove cases with low expressed isoforms:
#   #---------------------------------------------------------------------------------------------------
#
#   print (paste(nrow(data), "transcripts"))
#   print (paste(length(unique(gen)), "genes"))
#
#   totaldata = data  #### NEW!
#   totalgen = gen   #### NEW!
#
#   if (!is.null(minorFoldfilter)) {
#     print ("Removing low expressed minor isoforms")
#     moreOne <- names(which(table(gen) > 1))
#     iso.sel <- NULL
#     gene.sel <- NULL
#     for ( i in moreOne) {
#       which(gen==i)
#       gene.data <- data[which(gen==i),]
#       isoSUM <- apply(gene.data, 1, sum)
#       major <- names(which(isoSUM == max(isoSUM)))[1]
#       minors <- names(which(isoSUM != max(isoSUM)))
#       div <- as.numeric(matrix(rep(gene.data[major,], length(minors)), ncol = ncol(data), length(minors), byrow = T)) / as.matrix(gene.data[minors,])
#       is <- names(which(apply(div, 1, min, na.rm = T) < minorFoldfilter))
#       iso.sel <- c(iso.sel, major, is)
#       gene.sel <- c(gene.sel, rep(i, length(is)+1))
#     }
#
#     data <- data[iso.sel,]
#     gen <- gene.sel
#     print(dim(data))
#     print(length(gen))
#     print ("Done")
#   }
#
#   #---------------------------------------------------------------------------------------------------
#   #  STEP 0: Remove cases with 1 transcript:
#   #---------------------------------------------------------------------------------------------------
#
#   NT <- tapply(rownames(data),gen,length)
#   Genes1 <- names(which(NT==1))
#   data1 <- data[gen%in%Genes1,]
#   gen1 <- gen[gen%in%Genes1]
#   Genes1 <- unique(gen1)
#   print (paste(nrow(data1), "remaining transcripts to analyse DPA"))   # changed
#   print (paste(length(unique(Genes1)), "remaining genes to analyse DPA")) # changed
#
#   #NT <- tapply(rownames(data),gen,length)
#   #Genes1 <- names(which(NT!=1))
#   #data1 <- data[gen%in%Genes1,]
#   #gen1 <- gen[gen%in%Genes1]
#   #Genes1 <- unique(gen1)
#   #print (paste(nrow(data1), "remaining transcripts to analyse DPA"))   # changed
#   #print (paste(length(unique(Genes1)), "remaining genes to analyse DPA")) # changed
#
#   #---------------------------------------------------------------------------------------------------
#   # STEP 1: Gene models comparison.
#   #---------------------------------------------------------------------------------------------------
#
#   # make.design.matrix
#   results = NULL
#
#   if (ncol(design$edesign)==3){
#     results <- modelIso(Formula=Formula0, design, family, data1, Genes1, epsilon, Q, gen1)
#   }
#   else{
#     # for (k in 3:ncol(design$edesign)){
#     #   singleG_dis = design$edesign[which(design$edesign[,k]==1), c(1,2,k)]
#     #   dis = make.design.matrix(singleG_dis, degree = degree)
#     #   results[[colnames(design$edesign)[k]]] <- function(Formula=Formula0, dis, family, data1, Genes1, epsilon, Q)
#     # }
#     if(triple){Formula=Formula000} else {Formula=Formula00}
#     results <- modelIso(Formula=Formula, design, family, data1, Genes1, epsilon, Q, gen1)
#   }
#
#
#   #---------------------------------------------------------------------------------------------------
#   # STEP 2: p.vector and T.fit for DE to the transcripts that belong to selected.genes
#   #---------------------------------------------------------------------------------------------------
#   # data2 <- data[gen%in%selected.genes,]
#   # gen2 <- gen[gen%in%selected.genes]
#   # pvector2 <- p.vector(data2, design, counts=counts, item="isoform")
#   # Tfit2 <- T.fit(pvector2, item="isoform")
#   #
#   #---------------------------------------------------------------------------------------------------
#   # Output
#   #---------------------------------------------------------------------------------------------------
#
#   #ISO.SOL <-list(data, gen, design, selected.genes, pvector2, Tfit2)
#   #names(ISO.SOL)<- c("data","gen", "design", "DSG","pvector","Tfit")
#   #ISO.SOL
#
#
#   ISO.SOL <-list(totaldata, totalgen, design, results$selected.genes, results$p.adjusted)
#   names(ISO.SOL)<- c("data","gen", "design", "DSG", "adjpvalue")
#   ISO.SOL
#
# }
#
#
# #---------------------------------------------------------------------------------------------------
# # Auxiliar internal functions: Model, REP, Formula0, Formula1
# #---------------------------------------------------------------------------------------------------
#
# modelIso <- function(Formula, design, family, data1, Genes1, epsilon, Q, gen1){
#   pval<-NULL
#   g <- length(Genes1)
#   dis <- as.data.frame(design$dis)
#   mycolnames <- colnames(dis)
#   for(i in 1:g)
#   {
#     div <- c(1:round(g/100)) * 100
#     if (is.element(i, div))
#       print(paste(c("fitting gene", i, "out of", g), collapse = " "))
#
#     zz <-data1[gen1==Genes1[i],]
#     nt <- nrow(zz)
#
#     dis.gen <- REP(dis,nt)
#     y <-  c(t(as.matrix(zz)))
#     transcript<- factor(rep(c(1:nt),each = ncol(zz)))
#     ydis <- cbind(y, dis.gen, transcript)
#
#     model0 <- glm(Formula(mycolnames,design$edesign), data=ydis,  family = family, epsilon = epsilon )
#     model1 <- glm(Formula1(mycolnames), data=ydis,  family = family, epsilon = epsilon )
#
#     if(family$family=="gaussian")  {
#       pvali <- anova(model0, model1, test="F")[2,6] }
#     else {
#       pvali <- anova(model0,model1, test = "Chisq")[2,5] }
#     names(pvali) <- Genes1[i]
#     pval <- c(pval, pvali)
#   }
#   p.adjusted <- p.adjust(pval, method="fdr")
#   num.genes <- sum(p.adjusted<Q, na.rm=TRUE)
#   selected.genes <-names(sort(p.adjusted)[1:num.genes])
#
#   results = list(p.adjusted, selected.genes)
#   names(results) = c("p.adjusted", "selected.genes")
#
#   return(results)
#
# }
#
# #------------------------------------------------------------------------
#
# REP <- function(D,k)
# {
#   r<-nrow(D)
#   c<-ncol(D)
#   DD<-NULL
#   for(i in 1:c)
#   {
#     DDi <- rep(D[,i],k)
#     DD <- cbind(DD,DDi)
#   }
#   colnames(DD)<-colnames(D)
#   as.data.frame(DD)
# }
#
# #---------------------------------------------------------------------------
#
# Formula0 <- function(names,edesign=NULL)
# {
#   formula <- "y~"
#
#   if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
#   else if(length(names)>1)
#   {
#
#     for (i in 1:(length(names)))
#     {
#       formula <- paste(formula,names[i],"+")
#     }
#     formula <- paste(formula, "transcript")
#   }
#   formula <- as.formula(formula)
#   formula
# }
#
# #---------------------------------------------------------------------------
#
# Formula1 <- function(names)
# {
#   formula <- "y~"
#
#   if(length(names)==1){ formula=paste(formula,names[1],"* transcript") }
#   else if(length(names)>1)
#   {
#     formula <- paste(formula,"(" )
#     for (i in 1:(length(names)-1))
#     {
#       formula <- paste(formula,names[i],"+")
#     }
#     formula <- paste(formula,names[length(names)])
#     formula <- paste(formula, ") * transcript")
#   }
#   formula <- as.formula(formula)
#   formula
# }
#
# #---------------------------------------------------------------------------------------------------
# Formula000 <- function(names,edesign)
# {
#   formula <- "y~"
#   if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
#   else if(length(names)>1)
#   {
#     name.time = colnames(edesign)[1]
#     names.inter = paste(name.time,c("x","2x","3x","4x","5x"), sep="")
#     COL.inter=NULL
#     i=1
#     col.i = grep(names.inter[i],names)
#     COL.inter=col.i
#     while(length(col.i)!=0)
#     {
#       i=i+1
#       col.i = grep(names.inter[i],names)
#       COL.inter=c(COL.inter,col.i)
#     }
#     names1=names[-COL.inter]
#     names2=names[COL.inter]
#
#     formula <- paste(formula,"(" )
#     for (i in 1:(length(names1)-1))
#     {
#       formula <- paste(formula,names1[i],"+")
#     }
#     formula <- paste(formula,names1[length(names1)])
#     formula <- paste(formula, ") * transcript")
#   }
#
#   formula <- paste(formula, "+")
#
#   if(length(names2)>1){
#     for (i in 1:(length(names2)-1))
#     {formula <- paste(formula, names2[i],"+") }
#   }
#   formula <- paste(formula, names2[length(names2)])
#   formula <- as.formula(formula)
#   formula
# }
#
# #---------------------------------------------------------------
#
#
# Formula00 <- function(names,edesign)
# {
#   formula <- "y~"
#   if(length(names)==1){ formula=paste(formula,names[1],"+ transcript") }
#   else if(length(names)>1)
#   {
#     name.time = colnames(edesign)[1]
#     names.conds = colnames(edesign)[3:ncol(edesign)]
#     names.inter = paste(name.time,c("x","2x","3x","4x","5x"), sep="")
#     COL.group = unique(unlist(sapply(names.conds, function(m) grep(m, names))))
#     COL.inter = unique(unlist(sapply(names.inter, function(m) grep(m, names))))
#
#     COL.out = unique(c(COL.group,COL.inter))
#
#     names1=names[-COL.out]
#     names2=names[COL.out]
#
#     formula <- paste(formula,"(" )
#     for (i in 1:(length(names1)-1))
#     {
#       formula <- paste(formula,names1[i],"+")
#     }
#     formula <- paste(formula,names1[length(names1)])
#     formula <- paste(formula, ") * transcript")
#   }
#
#   formula <- paste(formula, "+")
#
#   if(length(names2)>1){
#     for (i in 1:(length(names2)-1))
#     {formula <- paste(formula, names2[i],"+") }
#   }
#   formula <- paste(formula, names2[length(names2)])
#   formula <- as.formula(formula)
#   formula
# }
#
# minorFoldfilterTappas <- function(data, gen, minorfilter, minorMethod=c("PROP","FOLD")){
#   print ("Removing low expressed minor isoforms")
#   moreOne <- names(which(table(gen) > 1))
#   iso.sel <- NULL
#   iso.sel.prop <- NULL
#
#   gene.sel <- NULL
#   if(minorMethod=="FOLD"){
#     for ( i in moreOne) {
#       which(gen==i)
#       gene.data <- data[which(gen==i),]
#       isoSUM <- apply(gene.data, 1, sum)
#       major <- names(which(isoSUM == max(isoSUM)))[1]
#       minors <- names(which(isoSUM != max(isoSUM)))
#       div <- as.numeric(matrix(rep(gene.data[major,], length(minors)), ncol = ncol(data), length(minors), byrow = T)) / as.matrix(gene.data[minors,])
#       is <- names(which(apply(div, 1, min, na.rm = T) < minorfilter))
#       iso.sel <- c(iso.sel, major, is)
#
#     }
#   }else{
#     for ( i in moreOne) {
#       which(gen==i)
#       gene.data <- data[which(gen==i),]
#
#       # by proportion
#       geneSUM <- apply(gene.data, 2, sum)
#       proportion = t(t(gene.data)/geneSUM)
#       is.prop = rownames(proportion[apply(proportion, 1, function(x) any(x>minorfilter)),,drop=F])
#       iso.sel <- c(iso.sel, is.prop)
#
#     }}
#
#   print(length(iso.sel))
#   print ("Done")
#   return(iso.sel)
# }

#
# Extra clusters funtions
#


my.PlotGroups <- function (data, edesign = NULL, time = edesign[, 1], groups = edesign[,
c(3:ncol(edesign))], repvect = edesign[, 2], show.fit = FALSE,
dis = NULL, step.method = "backward", min.obs = 2, alfa = 0.05,
nvar.correction = FALSE, summary.mode = "median", show.lines = TRUE,
groups.vector = NULL, xlab = "Time", ylab = "UTR Lengthening", filepath = "cluster",
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
cat("UTR Lengthening Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 14)
stop("Invalid number of arguments.")
method = ""
indir = ""
outdir = ""
filterFC = ""
minLength = ""
filteringType = ""
groupNames = ""

degValue = ""
kvalue = ""
mvalue = ""
cmpType = ""
restrictType = ""
sig = ""

for(i in 1:14) {
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
    else if(length(grep("^-u", args[i])) > 0)
    degValue = substring(args[i], 3)
    else if(length(grep("^-k", args[i])) > 0)
    kvalue = substring(args[i], 3)
    else if(length(grep("^-c", args[i])) > 0)
    mvalue = substring(args[i], 3)
    else if(length(grep("^-g", args[i])) > 0)
    cmpType = substring(args[i], 3)
    else if(length(grep("^-r", args[i])) > 0)
    restrictType = substring(args[i], 3)
    else if(length(grep("^-s", args[i])) > 0)
    sig = substring(args[i], 3)

    else {
        cat("Invalid command line argument: '", args[i], "'\n")
        stop("Invalid command line argument.")
    }
}
if(nchar(sig) == 0 || nchar(degValue) == 0 || nchar(kvalue) == 0 || nchar(mvalue) == 0 || nchar(cmpType) == 0 || nchar(restrictType) == 0 || nchar(groupNames) == 0 || nchar(outdir) == 0 || nchar(indir) == 0 || nchar(minLength) == 0 || nchar(method) == 0 || nchar(filterFC) == 0 || nchar(filteringType) == 0)
stop("Missing command line argument.")

#### determine what type of data to run UTRL for
minLength = as.numeric(minLength)
groupNames <- strsplit(groupNames, ";")[[1]]

sig = as.numeric(sig)

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

#method = "MASIGPRO"
#indir = "./"
#utrdir = "./"
#outdir = "./"
#minLength = "100"
#filteringType = "FOLD"
#mff = 2
#degree=3
#mclust=true
#kvalue=9
#knum = as.numeric(kvalue)
#minObs = (degree + 1) * (length(groups) + 1)
#sig = 0.05
#groupNames = c("NSC", "OLD")

# read expression factors definition table
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "time_factors.txt"), row.names=1, sep="\t", header=TRUE)
groups = unique(myfactors[,1])
times = length(unique(myfactors[,1]))
# read result matrix by transcripts
cat("\nReading normalized transcript matrix file data...")
transMatrix = read.table(file.path(indir, "result_matrix.tsv"), row.names=1, sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
#row.names(transMatrix) <- lapply(row.names(transMatrix), function(x) {gsub("GO:", "GO__", x)})
cat("\nRead ", nrow(transMatrix), " expression data rows")

# read info UTRL
cat("\nReading UTRL information data...")
infoUTRL = read.table(file.path(outdir, "utrl_info.tsv"), sep="\t", quote=NULL, header=TRUE,  stringsAsFactors=FALSE)
cat("\nRead ", nrow(transMatrix), " UTRL data rows")

design <- make.design.matrix(myfactors, degree)
n_groups = ncol(myfactors) - 2
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

infoUTRL = infoUTRL[which(infoUTRL$Trans %in% rownames(transMatrix)),]

#delete 1 transcript per gene after filtering
for(gene in unique(infoUTRL$Gene)){
    if(length(infoUTRL[infoUTRL$Gene==gene,"Gene"])==1)
    infoUTRL=infoUTRL[-which(infoUTRL$Gene==gene),]
}

#Filtrar UTR3 con menos de minLength
genesNotPolyA = NULL
for(gene in unique(infoUTRL$Gene)){
    length = infoUTRL[infoUTRL$Gene==gene,"UTR3"]
    max = max(length)
    min = min(length)
    if((max-min)<minLength)
    genesNotPolyA = rbind(genesNotPolyA, gene)
}

infoUTRL = infoUTRL[-which(infoUTRL$Gene %in% genesNotPolyA),]

##Calcular proporciones con matriz
transExpression = NULL
transPropportion = NULL
for(gene in unique(infoUTRL$Gene)){
    transExpression = data.frame(transMatrix[infoUTRL[infoUTRL$Gene==gene,]$Trans,])
    transPropportion = rbind(transPropportion, t(apply(transExpression, 1, function(x) x / colSums(transExpression))))
}

##Calcular peso de la UTR para cada gen

# wUTRgene = sum(prop_iso*UTRiso_gene) / n(isos)

wUTR3 = transPropportion * infoUTRL$UTR3
wUTR5 = transPropportion * infoUTRL$UTR5

wUTR3gene = NULL
wUTR5gene = NULL
for(gene in unique(infoUTRL$Gene)){
    #"Cdadc1"
    utrIsos3 = wUTR3[infoUTRL[infoUTRL$Gene==gene,]$Trans,]
    realUTR3 = infoUTRL[infoUTRL$Gene==gene,]$UTR3
    if(!any(is.nan(colSums(utrIsos3) / sum(realUTR3)))) #can be 0
      wUTR3gene = rbind(wUTR3gene, colSums(utrIsos3) / nrow(utrIsos3))
    else{
        aux <- colSums(utrIsos3) / sum(realUTR3)
        aux[which(is.nan(aux))] <- 0
        wUTR3gene = rbind(wUTR3gene, aux) #instead NaN keep 0s
    }

    utrIsos5 = wUTR5[infoUTRL[infoUTRL$Gene==gene,]$Trans,]
    realUTR5 = infoUTRL[infoUTRL$Gene==gene,]$UTR5
    if(!any(is.nan(colSums(utrIsos5) / sum(realUTR5)))) #can be 0
      wUTR5gene = rbind(wUTR5gene, colSums(utrIsos5) / sum(realUTR5))
    else{
        aux <- colSums(utrIsos5) / sum(realUTR5)
        aux[which(is.nan(aux))] <- 0
        wUTR5gene = rbind(wUTR5gene, aux) #instead NaN keep 0s
    }
}

rownames(wUTR3gene) <- unique(infoUTRL$Gene)
rownames(wUTR5gene) <- unique(infoUTRL$Gene)

#####
# TEST 3UTR Y 5UTR
#####

# run analysis UTR3 adn UTR5
results3 <-  p.vector(data.frame(wUTR3gene), design, Q = sig, MT.adjust = "BH", counts = FALSE)
results5 <-  p.vector(data.frame(wUTR5gene), design, Q = sig, MT.adjust = "BH", counts = FALSE)

#get intersection results3 and 5
eq <- nrow(results3$dat)==nrow(results5$dat)
if(!eq){
    cat("UTR3 and UTR5 have different lengths. Recalculating values...\n")
    m5 <- nrow(results5$dat) < nrow(results3$dat) #5 is minor
    if(m5){
        #wUTR5gene <- wUTR5gene[rownames(results5$dat),]
        wUTR3gene <- wUTR3gene[rownames(results5$dat),]
        results3 <-  p.vector(data.frame(wUTR3gene), design, Q = sig, MT.adjust = "BH", counts = FALSE)

    }else{
        wUTR5gene <- wUTR5gene[rownames(results3$dat),]
        #wUTR3gene <- wUTR3gene[rownames(results3$dat),]
        results5 <-  p.vector(data.frame(wUTR5gene), design, Q = sig, MT.adjust = "BH", counts = FALSE)
    }
}

results3$p.vector = signif(results3$p.vector, digits = 5)
results3$p.adjusted = signif(results3$p.adjusted, digits = 5)
results5$p.vector = signif(results5$p.vector, digits = 5)
results5$p.adjusted = signif(results5$p.adjusted, digits = 5)

#results3 <- IsoModel_tappas(data=data.frame(wUTR3gene), gen=unique(infoUTRL$Gene), design=design, degree=degree,
#                           counts=TRUE, min.obs = minObs, triple=triple) #minorFoldfilter = mff,

res3 = data.frame(rownames(results3$p.vector))
res3 = cbind(res3, results3$p.vector)
res3 = cbind(res3, results3$p.adjusted)
colnames(res3) <- c("Gene","PValue UTR3","Adj.PValue UTR3")

res5 = data.frame(results5$p.vector)
res5 = cbind(res5, results5$p.adjusted)
colnames(res5) <- c("PValue UTR5","Adj.PValue UTR5")

result_utrl <- cbind(res3,res5)
##########
write.table(result_utrl, file.path(outdir, "result.tsv"), quote=FALSE, row.names=FALSE, sep="\t")
##########

###########
# Plots CLUSTERS
###########

## Only significant genes
#ind = result_utrl[which(result_utrl[,"adjPValue 3UTR"]<=0.05),"gene"]
#ind5 = result_utrl[which(result_utrl[,"adjPValue 5UTR"]<=0.05),"gene"]

# generate cluster plots by groups
a = wUTR3gene
for(i in 1:(ncol(design$edesign)-2)) {
    gname <- names(design$edesign[i+2])
    cat("\nGenerating cluster graphs for ", gname, "...\n")
    filepath=file.path(outdir, paste0("cluster_", gname, "_UTR3"))
    cmethod = "hclust"
    if(isTRUE(usemclust))
        cmethod = "Mclust"
    cat("\nclusterMethod: ", cmethod, ", useMclust: ", usemclust)
    num_col = c(1,2,which(colnames(design$edesign)==gname))
    print(n_groups)
    print(1+i)
    print(ncol(design$dis)-1)
    #num_col_dis = c(1,seq(1+i,ncol(design$dis)-1, by = n_groups))
    num_col_dis <- NULL

    cont = 1
    new_design = design
    for(j in unique(new_design$edesign[as.logical(new_design$edesign[,gname]),num_col]$Replicate)){
        new_design$edesign[as.logical(new_design$edesign[,gname] & new_design$edesign$Replicate==j),num_col]$Replicate = cont
        cont = cont+1
    }

    clusters <- my.see.genes(a[,as.logical(new_design$edesign[,gname])], edesign=new_design$edesign[as.logical(new_design$edesign[,gname]),num_col], show.fit = F, dis=new_design$dis[as.logical(new_design$edesign[,gname]),num_col_dis], cluster.method=cmethod, cluster.data = 1, k=knum, k.mclust=usemclust, min.obs = 0.001, newX11 = FALSE, filepath = filepath)
    if(!is.null(clusters)) {
        if(!is.null(clusters$cluster.algorithm.used)) {
            # create a data frame with cluster information and save
            dfc <- data.frame(clusters$cut)
            rownames(dfc) <- rownames(a)
            head(dfc)
            cat("\nSaving UTRL clusters data to file...")
            write.table(dfc, file.path(outdir, paste0("cluster_", gname, "_UTR3", ".tsv")), quote=FALSE, row.names=TRUE, sep="\t")
        } else {
            cat("\nWARNING: Unable to generate clusters (something is.null).\n")
        }
    } else {
        cat("\nWARNING: Unable to generate clusters.\n")
    }
}

a = wUTR5gene
for(i in 1:(ncol(design$edesign)-2)) {
    gname <- names(design$edesign[i+2])
    cat("\nGenerating cluster graphs for ", gname, "...\n")
    filepath=file.path(outdir, paste0("cluster_", gname, "_UTR5"))
    cmethod = "hclust"
    if(isTRUE(usemclust))
        cmethod = "Mclust"
    cat("\nclusterMethod: ", cmethod, ", useMclust: ", usemclust, "\n")
    num_col = c(1,2,which(colnames(design$edesign)==gname))
    #num_col_dis = c(1,seq(1+i,ncol(design$dis)-1, by = n_groups))
    num_col_dis <- NULL

    cont = 1
    new_design = design
    for(j in unique(new_design$edesign[as.logical(new_design$edesign[,gname]),num_col]$Replicate)){
        new_design$edesign[as.logical(new_design$edesign[,gname] & new_design$edesign$Replicate==j),num_col]$Replicate = cont
        cont = cont+1
    }

    clusters <- my.see.genes(a[,as.logical(new_design$edesign[,gname])], edesign=new_design$edesign[as.logical(new_design$edesign[,gname]),num_col], show.fit = F, dis=new_design$dis[as.logical(new_design$edesign[,gname]),num_col_dis], cluster.method=cmethod, cluster.data = 1, k=knum, k.mclust=usemclust, min.obs = 0.001, newX11 = FALSE, filepath = filepath)
    if(!is.null(clusters)) {
        if(!is.null(clusters$cluster.algorithm.used)) {
            # create a data frame with cluster information and save
            dfc <- data.frame(clusters$cut)
            rownames(dfc) <- rownames(a)
            head(dfc)
            cat("\nSaving UTRL clusters data to file...")
            write.table(dfc, file.path(outdir, paste0("cluster_", gname, "_UTR5", ".tsv")), quote=FALSE, row.names=TRUE, sep="\t")
        } else {
            cat("\nWARNING: Unable to generate clusters (something is.null).\n")
        }
    } else {
        cat("\nWARNING: Unable to generate clusters.\n")
    }
}

# write completion file
cat("\nWriting UTRL Analysis completed file...")
filedone <- file(file.path(outdir, "done.txt"))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")


#####
# OLDWAY
#####

#####
# Graphic
#####

#Diferencia de group2-group1

# utrl_results3 <- data.frame(
#   group = rep(c("UTR3"), each = length(utrWeight3))
# )
# utrl_results5 <- data.frame(
#   group = rep(c("UTR5"), each = length(utrWeight5))
# )
#
# nm = c("group")
# for(time in 1:(length(groups)-1)){
#   nm <- cbind(nm, paste0(time+1," vs ",time))
#   utrl_results3 = cbind(utrl_results3, as.data.frame(c(as.numeric(aux3[time+1,])-as.numeric(aux3[time,]))))
#   utrl_results5 = cbind(utrl_results5, as.data.frame(c(as.numeric(aux5[time+1,])-as.numeric(aux5[time,]))))
# }
#
# colnames(utrl_results3) <- nm
# colnames(utrl_results5) <- nm
# utrl_results = rbind(utrl_results3, utrl_results5)
#
# minimum = min(
#   as.numeric(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[2,2], " ")[[1]][4]),
#   as.numeric(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[2,2], " ")[[1]][4])
# )
#
# maximum = max(
#   as.numeric(strsplit(summary(utrl_results[utrl_results$group=="UTR3",])[5,2], " ")[[1]][5]),
#   as.numeric(strsplit(summary(utrl_results[utrl_results$group=="UTR5",])[5,2], " ")[[1]][5])
# )

#
# for(time in 2:ncol(utrl_results)){
#   ggp <- ggplot(utrl_results, aes(x=group, y=utrl_results[,time], fill = group)) +
#     geom_boxplot(notch=TRUE) +
#     ylim(minimum-80,maximum+80) +
#     ylab(label = "wUTR Difference") +
#     xlab(label = paste0("UTR ", colnames(utrl_results)[time])) +
#     # Use custom color palettes
#     scale_fill_manual(values=c("#f2635c", "#7cafd6")) +
#     guides(fill=guide_legend(title="UTR")) +
#     theme_bw()
#   #theme(panel.background = element_rect(fill = "white"))
#
#   ggsave(paste0(outfile, paste0("UTR ", colnames(utrl_results)[time]),".png"), plot=ggp, device="png", bg="transparent", width=5, height=5, dpi=150)
#
# }
