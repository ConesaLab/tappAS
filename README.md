# tappAS: a user-friendly application to analyze the functional implications of alternative splicing.

This repository contains the source code of the tappAS application. 

TappAS is a bioinformatics application containing a wide range of analyses to evaluate the functional
impact of Alternative Transcript Expression Mechanisms (AltTEM), in the form of a GUI application 
developed in Java and R. To do this, tappAS relies on A) a series of statistical methods to evaluate different aspects
of post-transcriptional processing and B) a functional annotation file containing positional information for functional
motifs and domains at the transcript and CDS levels.

[![DOI](https://zenodo.org/badge/255313071.svg)](https://zenodo.org/badge/latestdoi/255313071).

### Installation
This source code is available as a compiled, user-ready application.
To obtain the latest version of the tappAS, please acess the [Releases section](https://github.com/ConesaLab/tappAS/releases) 
and download the *tappAS.jar* file.

For further installation instructions and information on compatibility 
and dependencies, see [the Install file](https://app.tappas.org/resources/downloads/install.pdf) or visit the 
[tappAS website](https://app.tappas.org/).

**WARNING: any release of tappAS < 1.0.0 constitutes a beta version. Use at your own risk.** 
If you were using version 0.99.15 or below, we recommend replacing it with tappAS 1.0.0 or above. 
In this case, due to major changes in the application functioning, you'll need to delete the 
*tappasWorkspace* folder and create all projects again. We apologize for any inconveniences.

### Documentation
Documentation and videotutorials can be found at https://app.tappas.org/ (site under development).

Comprehensive information about the methods and graphics implemented in tappAS,
their interpretation and how to run them can be found within the application. Each window and panel
contains a **Help Menu** as a source of built-in documentation. To access it, 
click the [**?**] icon available in the different windows and panels.

### Contact
If you encounter a bug or error using or installing tappAS, 
please [open an Issue](https://github.com/ConesaLab/tappAS/issues) to report it.

If in need of further support, please contact:
* **Pedro Salguero:** installation information, errors and software-related support: pedsalga@upv.edu.es
* **Ángeles Arzalluz-Luque:** information related to analysis/results interpretation and questions on 
how to apply tappAS functionalities to your data: angeles.arzalluz@gmail.com


### License and copyright 
Copyright (C) 2020, de la Fuente et al.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

A copy of the GNU General Public License
is provided along with this program (see LICENSE file).
