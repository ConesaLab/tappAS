pkg_bioc = c("DEXSeq","edgeR","NOISeq","goseq","maSigPro")
pkg_cran = c("callr", "devtools","ggplot2","MASS","plyr","VennDiagram", "ggrepel","cowplot","tidyverse","UpSetR")
pkg_gthb = c("GOglm")

if(length(setdiff(pkg_bioc, rownames(installed.packages()))) == 0 & 
   length(setdiff(pkg_cran, rownames(installed.packages()))) == 0 & 
   length(setdiff(pkg_gthb, rownames(installed.packages()))) == 0){
  message("TRUE")
}else{
  message("FALSE")
}
