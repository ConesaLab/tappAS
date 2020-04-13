# Time Course Differential Expression Analysis R script
#
# Using maSigPro R package
#
# Note:   All arguments are required.
#
# Written by H. del Risco

library("maSigPro")
library("mclust")

# run time course DEA analysis
time_analysis <- function(data, factors, degree, siglevel, r2cutoff, knum, usemclust, groups, outdir) {
    design <- make.design.matrix(factors, degree)
    minobs = (degree + 1) * groups
    cat("\ngroups: ", groups, ", degree: ", degree, ", minobs: ", minobs)
    cat("\nsiglevel: ", siglevel, ", r2cutoff: ", r2cutoff, ", knum: ", knum, ", useMclust: ", usemclust)
    print(design$edesign)
    cat("\nRunning p.vector()...\n")
    fit <- p.vector(data, design, Q=siglevel, MT.adjust="BH", min.obs=minobs, counts=TRUE)
    cat("\nFound ", fit$i, " out of ", fit$g, " items to be significant\n")
    cat("\ndesign:\n")
    print(head(fit$SELEC))
    cat("\nRunning T.fit() using step.method='backward' and alfa = ", siglevel, "...\n")
    tstep <- T.fit(fit, step.method="backward", alfa=siglevel)
    cat("\nRunning get.siggenes() using rsq=", r2cutoff, " and vars='groups'...\n")
    sigs <- get.siggenes(tstep, rsq=r2cutoff, vars="groups")
    # generate cluster plots files
    for(i in 1:length(names(sigs$sig.genes))) {
        gname <- names(sigs$sig.genes)[i]
        cat("\nGenerating cluster graphs for ", gname, "...\n")
        filepath=file.path(outdir, paste0("cluster_", dataType, "_", gname))
        cmethod = "hclust"
        if(isTRUE(usemclust))
            cmethod = "Mclust"
        cat("\nclusterMethod: ", cmethod, ", useMclust: ", usemclust)
        clusters <- my.see.genes(sigs$sig.genes[[gname]], show.fit = T, dis=design$dis, cluster.method=cmethod, cluster.data = 1, k=knum, k.mclust=usemclust, newX11 = FALSE, filepath=filepath)
        if(!is.null(clusters)) {
            if(!is.null(clusters$cluster.algorithm.used)) {
                # create a data frame with cluster information and save
                dfc <- data.frame(clusters$cut)
                head(dfc)
                cat("\nSaving ", dtname, " DEA clusters data to file...")
                write.table(dfc, file.path(outdir, paste0("cluster_", dataType, "_", gname, ".tsv")), quote=FALSE, row.names=TRUE, sep="\t")
            } else {
                cat("\nWARNING: Unable to generate clusters (algo is.null).\n")
            }   
        } else {
            cat("\nWARNING: Unable to generate clusters.\n")
        }
    }
    return(sigs)
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
    pdf(NULL)

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
    groups.vector = NULL, xlab = "Time", ylab = "Expression value", filepath = "cluster",
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
            sub <- paste0("Median profile of ", nrow(data), " ", dataType, "s")
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
    #ggsave("/home/hdelrisco/Documents/plot.png", plot=ggp, device="png", bg="transparent", width=4, height=3, dpi=300)
}

# handle command line arguments - don't want to use external package so users don't have to install it
args = commandArgs(TRUE)
cat("Differential Expression Analysis script arguments: ", args, "\n")
arglen = length(args)
if(arglen != 9)
  stop("Invalid number of arguments.")
dataType = ""
method = ""
r2value = ""
sigvalue = ""
degvalue = ""
kvalue = ""
mvalue = ""
indir = ""
outdir = ""
for(i in 1:9) {
    if(length(grep("^-d", args[i])) > 0)
      dataType = substring(args[i], 3)
    else if(length(grep("^-m", args[i])) > 0)
      method = substring(args[i], 3)
    else if(length(grep("^-r", args[i])) > 0)
      r2value = substring(args[i], 3)
    else if(length(grep("^-s", args[i])) > 0)
      sigvalue = substring(args[i], 3)
    else if(length(grep("^-u", args[i])) > 0)
      degvalue = substring(args[i], 3)
    else if(length(grep("^-k", args[i])) > 0)
      kvalue = substring(args[i], 3)
    else if(length(grep("^-c", args[i])) > 0)
      mvalue = substring(args[i], 3)
    else if(length(grep("^-i", args[i])) > 0)
      indir = substring(args[i], 3)
    else if(length(grep("^-o", args[i])) > 0)
      outdir = substring(args[i], 3)
    else {
      cat("Invalid command line argument: '", args[i], "'\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(outdir) == 0 || nchar(indir) == 0 || nchar(dataType) == 0 || nchar(degvalue) == 0 || nchar(sigvalue) == 0 || nchar(r2value) == 0 || nchar(kvalue) == 0 || nchar(mvalue) == 0 || nchar(method) == 0)
  stop("Missing command line argument.")

# determine what type of data to run DEA for
dtname <- ""
if(dataType == "gene") {
    dtname <- "gene"
} else if(dataType == "protein") {
    dtname <- "protein"
} else {
    dtname <- "transcript"
}
r2cutoff = as.numeric(r2value)
siglevel = as.numeric(sigvalue)
degree = as.numeric(degvalue)
knum = as.numeric(kvalue)
usemclust = TRUE
if(mvalue == "0")
    usemclust = FALSE

# read expression factors
cat("\nReading factors file data...")
myfactors=read.table(file.path(indir, "time_factors.txt"), row.names=1, sep="\t", header=TRUE)
groups = ncol(myfactors) - 2
times = length(unique(myfactors[,1]))
#degree = times - 1

# read expression matrix
cat("\nCalculating ", dtname, " differential expression...\n")
cat("\nReading normalized ", dtname, " matrix file data...")
expMatrix = read.table(file.path(indir, paste0(dtname, "_matrix.tsv")), row.names=1, sep="\t", header=TRUE)
cat("\nRead ", nrow(expMatrix), " normalized ", dtname, " expression data rows")

# run analysis
sigs = time_analysis(data=expMatrix, factors=myfactors, degree=degree, siglevel=siglevel, r2cutoff=r2cutoff, knum=knum, usemclust=usemclust, groups=groups, outdir=outdir)
for(i in 1:length(names(sigs$sig.genes))) {
    gname <- names(sigs$sig.genes)[i]
    cat("\n", gname, " significant ", dtname, ": ")
    print(sigs$sig.genes[[gname]]$g)
    df <- data.frame(sigs$sig.genes[[gname]]$sig.pvalues[, 1:2])
    # df <- df[order(df$p.value), ]
    print(head(df))
    cat("\nSaving ", dtname, " DEA results to file...")
    write.table(df, file.path(outdir, paste0("result_", dataType, "_", gname, ".tsv")), quote=FALSE, row.names=TRUE, sep="\t")
}

# write completion file
cat("\nWriting DEAnalysis completed file...")
filedone <- file(file.path(outdir, paste("done_", dataType, ".txt", sep="")))
writeLines("end", filedone)
close(filedone)
cat("\nAll done.\n")
