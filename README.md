# Incremental Conformance Checking
#### Project Status: [Active]

This repository contains the source code, the end-user plugins and the evaluation result files for incremental conformance checking, as proposed in the paper ["Estimating Process Conformance by Trace Sampling and Result Approximation"](https://www.researchgate.net/publication/333209975_Estimating_Process_Conformance_by_Trace_Sampling_and_Result_Approximation).

The approaches provided include:
* Three conformance measures: Fitness, deviating activities and non-conformant resource attributes
* Event-log sampling with statistical completeness guarantees#
* Methods for the approximation of said conformance measures
* Quality retainment approaches to further stabilize the sample-based conformance result.

The methods are implemented as end-user plugins for the [Process Mining Toolkit](http://www.promtools.org/doku.php). The repository contains all configuration files for the local development and execution of ProM plugins using the Eclipse IDE.

## Getting started
### Installation
1. Clone this repo (for help see this [tutorial](https://help.github.com/articles/cloning-a-repository/)).
2. Install the [Eclipse IDE](https://www.eclipse.org/downloads/) and import th IncrementalPM directory as a new project.
3. In Eclipse, install [Apache IvyDE](https://ant.apache.org/ivy/ivyde/), and resolve all dependencies of the imported project.
4. You are done. You may run your local copy of ProM using either "ProM Package Manager (IncrementalPM).launch" for the package Manager or "ProM with UITopia (IncrementalPM).launch" for ProM itself.

For further instructions on the usage of ProM itself, or the deployment using other IDE's than Eclipse, please see the [ProM Getting started Page](http://www.promtools.org/doku.php?id=gettingstarted:start) or consider contacting the [ProM Forum](https://www.win.tue.nl/promforum/categories).

### Running the plugins
In the project, two plugins are provided:
* "Check global Conformance with Incremental Conformance Checker" - conducts a run of the sample-based conformance checking algorithm
* "Evaluate Incremental Conformance Checker" - conducts the controlled experiments used for evaluation of the implemented approaches. The set of result files used in the paper, as well as the scripts for the creation of the plots are provided in the directory "evaluation_results".

## Acknowledgements
This work has received funding from the Deutsche Forschungsgemeinschaft [DFG](https://www.dfg.de/), grant number [421921612](https://gepris.dfg.de/gepris/projekt/421921612?context=projekt&task=showDetail&id=421921612&), and the [Alexander von Humboldt Foundation](http://www.humboldt-foundation.de/web/start.html).

## Contact
martin.bauer@hu-berlin.de

### License
We provide our code, under the MIT license.
