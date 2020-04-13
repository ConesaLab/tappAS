# Generate barplot plot for FDA combined results Analysis
#
# WARNING: This script is intended to be used directly by the tappAS application ONLY
#          Some of the file names are hard coded to avoid having to pass all names as arguments
#
# Script arguments:
#   input FDA folder
#   input analayses ID
#   output plot: full path to output plot image file
#
# Note:   All arguments are required. Expression matrix must be in raw counts.
#
# Written by Pedro Salguero - Ángeles Arzalluz

# LOAD DATA
library(tidyverse)
library(cowplot)
theme_set(theme_cowplot())
args = commandArgs(TRUE)
cat("FDA Combine Results - Barplot: ", args, "\n")
arglen = length(args)
if(arglen != 4)
  stop("Invalid number of arguments.")

infile = ""
a1 = ""
a2 = ""
outfile = ""

for(i in 1:4) {
  if(length(grep("^-i", args[i])) > 0)
    infile = substring(args[i], 3)
  else if(length(grep("^-g", args[i])) > 0)
    a1 = substring(args[i], 3)
  else if(length(grep("^-p", args[i])) > 0)
    a2 = substring(args[i], 3)
  else if(length(grep("^-o", args[i])) > 0)
    outfile = substring(args[i], 3)
  else {
    cat("Invalid command line argument: '", args[i], "'\n")
    stop("Invalid command line argument.")
  }
}
if(nchar(a1) == 0 || nchar(a2) == 0 || nchar(outfile) == 0 || nchar(infile) == 0)
  stop("Missing command line argument.")

positional <- read_tsv(file.path(infile, paste0("result_id.",a1,".tsv"))) %>% arrange(desc(`Varying`))
presence <- read_tsv(file.path(infile, paste0("result_id.",a2,".tsv"))) %>% arrange(desc(`Varying`))

# SELECT TOP-15 IDs
positional_top <- positional[1:15,]
presence_top <- presence %>% filter(`#FeatureID` %in% positional_top$`#FeatureID`)

# CREATE PLOT DATA FRAME
varying_count <- bind_rows(Positional = positional_top %>% select(`Description`, `Varying`), 
                           Presence = presence_top %>% select(`Description`, `Varying`),
                           .id = "Approach")

# PLOT
ylabel <- "No. of genes with varying"
title <- "ID-level variation using both FDA approaches"

ggp <- ggplot(varying_count) + ggtitle(title) + 
  geom_bar(aes(x = `Description`, y = `Varying`, fill = Approach), 
           stat = "identity", position = "dodge", color = "black") +
  theme(axis.text.x = element_text(angle = 55, hjust = 1, size = 14, color = "grey30"), plot.margin = unit(c(1,0,0,2), "cm")) +
  xlab("") + ylab(ylabel) + scale_fill_manual(values = c("#1DA519B5", "#31497D"))

ggsave(outfile, plot=ggp, device="png", bg="white", width=10, height=7.5, dpi=120)
cat("\nAll done.\n")