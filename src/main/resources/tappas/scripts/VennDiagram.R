# Draw VennDiagram for given data
#
# WARNING: This script is intended to be used directly by the TAPPAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input matrix: full path to matrix file
#   output file: full path to venn diagram output image file
#   set count: number of data sets
#   set names: data set display names
#   intersect counts: intersection counts
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by H. del Risco


library("VennDiagram")

# handle command line arguments - don't want to use external package so users don't have to install it

args = commandArgs(TRUE)
cat("VennDiagram arguments: ", args, "\n")
arglen = length(args)
if(arglen != 4)
  stop("Invalid number of arguments.")
outFile = ""
setscnt = ""
names = ""
values = ""
for(i in 1:4) {
    if(length(args[i]) > 0) {
      if(i == 1)
          outFile = args[i]
      else if(i == 2)
          setscnt = args[i]
      else if(i == 3)
          names = substr(args[i], 2, nchar(args[i]) - 1)
      else if(i == 4)
          values = args[i]
    }
    else {
      cat("Invalid command line arguments.\n")
      stop("Invalid command line argument.")
    }
}
if(nchar(outFile) == 0 || nchar(setscnt) == 0 || nchar(names) == 0 || nchar(values) == 0)
  stop("Missing command line argument.")

nsets = as.numeric(setscnt)
lsnames <- strsplit(names, "[;]")
lsvals <- strsplit(values, "[,]")

# generate VennDiagram plot
if(nsets == 2) {
    venn.plot <- draw.pairwise.venn(area1 = as.numeric(lsvals[[1]][1]), area2 = as.numeric(lsvals[[1]][2]), cross.area = as.numeric(lsvals[[1]][3]), category = lsnames[[1]], fill = c("green", "yellow"), alpha = rep(0.15, 2), cat.cex = 2, cex = 2, cat.pos = 0, margin = 0.05)
} else if(nsets == 3) {
    venn.plot <- draw.triple.venn(as.numeric(lsvals[[1]][1]), as.numeric(lsvals[[1]][2]), as.numeric(lsvals[[1]][3]), as.numeric(lsvals[[1]][4]), as.numeric(lsvals[[1]][5]), as.numeric(lsvals[[1]][6]), as.numeric(lsvals[[1]][7]), lsnames[[1]], fill = c("green", "yellow", "blue"), alpha = rep(0.15, 3), cat.cex = 2, cex = 2, cat.pos = c(350, 10, 180), margin = 0.05)
} else if(nsets == 4) {
    venn.plot <- draw.quad.venn(as.numeric(lsvals[[1]][1]), as.numeric(lsvals[[1]][2]), as.numeric(lsvals[[1]][3]), as.numeric(lsvals[[1]][4]), as.numeric(lsvals[[1]][5]), as.numeric(lsvals[[1]][6]), as.numeric(lsvals[[1]][7]), as.numeric(lsvals[[1]][8]), as.numeric(lsvals[[1]][9]), as.numeric(lsvals[[1]][10]), as.numeric(lsvals[[1]][11]), as.numeric(lsvals[[1]][12]), as.numeric(lsvals[[1]][13]), as.numeric(lsvals[[1]][14]), as.numeric(lsvals[[1]][15]), lsnames[[1]], fill = c("green", "yellow", "blue", "red"), alpha = rep(0.15, 4), cat.cex = 2, cex = 2, cat.pos = 0, margin = 0.05)
} else if(nsets == 5) {
    venn.plot <- draw.quintuple.venn(as.numeric(lsvals[[1]][1]), as.numeric(lsvals[[1]][2]), as.numeric(lsvals[[1]][3]), as.numeric(lsvals[[1]][4]), as.numeric(lsvals[[1]][5]), as.numeric(lsvals[[1]][6]), as.numeric(lsvals[[1]][7]), as.numeric(lsvals[[1]][8]), as.numeric(lsvals[[1]][9]), as.numeric(lsvals[[1]][10]), as.numeric(lsvals[[1]][11]), as.numeric(lsvals[[1]][12]), as.numeric(lsvals[[1]][13]), as.numeric(lsvals[[1]][14]), as.numeric(lsvals[[1]][15]), 
                    as.numeric(lsvals[[1]][16]), as.numeric(lsvals[[1]][17]), as.numeric(lsvals[[1]][18]), as.numeric(lsvals[[1]][19]), as.numeric(lsvals[[1]][20]), as.numeric(lsvals[[1]][21]), as.numeric(lsvals[[1]][22]), as.numeric(lsvals[[1]][23]), as.numeric(lsvals[[1]][24]), as.numeric(lsvals[[1]][25]), as.numeric(lsvals[[1]][26]), as.numeric(lsvals[[1]][27]), as.numeric(lsvals[[1]][28]), as.numeric(lsvals[[1]][29]), as.numeric(lsvals[[1]][30]), as.numeric(lsvals[[1]][31]), 
                    lsnames[[1]], fill = c("green", "yellow", "blue", "red", "#00FFFF"), alpha = rep(0.15, 5), cat.cex = 2, cex = 2, cat.pos = c(0, 0, 180, 180, 0), margin = 0.05)
} else {
  cat("Invalid number of sets.\n")
  stop("Invalid number of sets.")
}

# generate output image
png(file=outFile, width=800, height=800, units="px", bg="white")
    grid.draw(venn.plot)
dev.off()
cat("\nAll done.\n")
