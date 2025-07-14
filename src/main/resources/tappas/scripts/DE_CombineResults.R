library(cowplot)
library(tidyverse)
library(UpSetR)
theme_set(theme_cowplot())

# LOAD THE DATA
grouped <- map(paths, read_tsv)
names(grouped) <- set_names

# INTERSECT
intersections <- fromList(grouped)

# PLOT
# select colors for bars from palette
colors <- c("#f2635c","#7cafd6","#a2ca72","#f7c967","#f9f784","#a16cc1","#a796ff","#ff96eb")
bar_color <- colors[1]
select_colors <- colors[2:ncol(intersections)+1]

# set legends (generic, FDA and Differential)
if(is.null(arg[x]) == TRUE){
  sets_xlabel <- "No. of elements in group"
  main_ylabel <- "No. of common elements"
} else if(arg[x] == "FDA"){
  sets_xlabel <- "No. of varying genes"
  main_ylabel <- "No. of common varying genes"
} else if(arg[x] == "Differential"){
  sets_xlabel <- "No. of genes"
  main_ylabel <- "No. of common detected genes"
}

# upset plot
upset(intersections, nsets = 10,
      keep.order = TRUE, text.scale = c(1.7, 1.5, 1.3, 1.3, 1.7, 2), 
      sets.bar.color = c(select_colors), point.size = 3, order.by = "freq", main.bar.color = bar_color,
      sets.x.label = sets_xlabel, mainbar.y.label = main_ylabel, 
      matrix.dot.alpha = 0, shade.color = "gray60")