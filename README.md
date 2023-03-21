# Incremental Conformance Checking

This repository contains the source code, the end-user plugins and the evaluation result files for incremental conformance checking, as proposed in the paper ["Sampling and Approximation Techniques for efficient Process Conformance Checking"](https://www.researchgate.net/publication/347447789_Sampling_and_approximation_techniques_for_efficient_process_conformance_checking).

The approaches provided include:
* Three conformance measures: Fitness, deviating activities and resource attributes related to non-conformant behaviour (only available from code)
* Event-log sampling with statistical completeness guarantees
* Methods for the approximation of said conformance measures
* Quality retainment approaches to further stabilize the sample-based conformance result. (only available from code)

The methods are implemented as end-user plugins for the [Process Mining Toolkit](http://www.promtools.org/doku.php). The repository contains all configuration files for the local development and execution of ProM plugins using the Eclipse IDE.


## Getting started
### Installation
1. Clone this repo (for help see this [tutorial](https://help.github.com/articles/cloning-a-repository/)).
2. Install the [Eclipse IDE](https://www.eclipse.org/downloads/) and import the IncrementalPM directory as a new project.
3. In Eclipse, install [Apache IvyDE](https://ant.apache.org/ivy/ivyde/), and resolve all dependencies of the imported project.
4. You are done - you may run your local copy of ProM using either "ProM Package Manager (IncrementalPM).launch" for the package Manager or "ProM with UITopia (IncrementalPM).launch" for ProM itself. 

Note, that you do not need to install the standalone version of ProM, as the project already includes a local copy of ProM.
For further instructions on the usage of ProM itself, or the deployment using other IDE's than Eclipse, please see the [ProM Getting started Page](http://www.promtools.org/doku.php?id=gettingstarted:start) or consider contacting the [ProM Forum](https://www.win.tue.nl/promforum/categories).

### Running the plugins
In the project, two plugins are provided:
* "Check sample-based Conformance using Incremental Conformance Checking" - conducts a run of the sample-based conformance checking algorithm
* "Evaluate Incremental Conformance Checker" - conducts the controlled experiments used for evaluation of the implemented approaches. The set of result files used in the paper, as well as the scripts for the creation of the plots are provided in the directory "evaluation_results".


## Evaluation Results
The result files, figures and scripts plotting those figures, used for the evaluation in the paper, are located in the directory "Evaluation". In the directory the top-level python script "plot.py" generates a set of plots based on the .csv-files located in the directory "csv", which have been generated using the aforementioned evaluation-plugin. For the sake of clarity, the outputted plots are arranged into directories based on corresponding data set.


## Acknowledgements
This work has received funding from the Deutsche Forschungsgemeinschaft [DFG](https://www.dfg.de/), grant number [421921612](https://gepris.dfg.de/gepris/projekt/421921612?context=projekt&task=showDetail&id=421921612&), and the [Alexander von Humboldt Foundation](http://www.humboldt-foundation.de/web/start.html).

## Contact
martin.kabierski@hu-berlin.de

### License
We provide our code, under the MIT license.
