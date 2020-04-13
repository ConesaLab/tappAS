##  Packages for tappAS

##  Packages installed using biocLite()
pkg_bioc = c("DEXSeq","edgeR","NOISeq","goseq","mdgsa", "maSigPro")

##  Packages installed using biocLite() [devtools is used to install github packages]
pkg_cran = c("rlang","callr", "devtools","ggpubr","ggplot2","MASS","plyr","VennDiagram", "ggrepel","cowplot","tidyverse","UpSetR")

##  Packages installed using github
pkg_gthb = c("GOglm")
pkg_gthb_install = c("gu-mi/GOglm")
installation = FALSE
tryCatch(
  expr = {
    v = as.numeric(paste0(R.Version()$major, R.Version()$minor)) #36.6
    
    if(v < 35){
      ## Installation Cran
      if (length(setdiff(pkg_cran, rownames(installed.packages()))) > 0) {
        install.packages(setdiff(pkg_cran, rownames(installed.packages())),repos = "https://cloud.r-project.org/")
      }
      
      # Installation Bioconductor
      if (length(setdiff(pkg_bioc, rownames(installed.packages()))) > 0) {
        source("http://www.bioconductor.org/biocLite.R")
        biocLite(setdiff(pkg_bioc, rownames(installed.packages())), suppressUpdates=TRUE)
      }
      
      ## Installation Github
      if (length(setdiff(pkg_gthb, rownames(installed.packages()))) > 0) {
        library(devtools)
        devtools::install_github(setdiff(pkg_gthb_install, rownames(installed.packages())))
      }
      installation = TRUE
    }else{
      ##  Packages installed using biocLite() [devtools is used to install github packages]
      pkg_cran = c("BiocManager", pkg_cran)
      
      ## Installation Cran
      if (length(setdiff(pkg_cran, rownames(installed.packages()))) > 0) {
        install.packages(setdiff(pkg_cran, rownames(installed.packages())), repos = "https://cran.rediris.es/")
      }
      
      # Installation Bioconductor
      if (length(setdiff(pkg_bioc, rownames(installed.packages()))) > 0) {
        lst = setdiff(pkg_bioc, rownames(installed.packages()))
        for(p in lst){
          library(BiocManager)
          BiocManager::install(p, ask=FALSE)
        }
      }
      
      ## Installation Github
      if (length(setdiff(pkg_gthb, rownames(installed.packages()))) > 0) {
        library(devtools)
        devtools::install_github(setdiff(pkg_gthb_install, rownames(installed.packages())), quiet = T)
      }
    }
    installation = TRUE
  },
  error = function(e){ 
    installation = FALSE
  }
)

message(installation)