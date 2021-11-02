# LocPrivFogSim

[LocPrivFogSim](https://git.uni-due.de/sjmsschl/locprivfogsim) is a simulator which has gone through a lot of modifications and extensions in the past. Starting with CloudSim,
an open-source framework for modelling and simulating cloud computing infrastructures and services, the simulator was first
modified to cover fog infrastructures and applications and renamed iFogSim [1]. From there on, iFogSim was extended to
cover mobile end-devices with geographic positions, and more. This modification was renamed MobFogSim [2]. Lastly,
MobFogSim was extended by T. Wettig [3] at a conducted bachelor’s thesis at the University Duisburg-Essen in the year 2020
to cover simulations of location privacy in fog computing. In this repo, the version of [LocPrivFogSim](https://git.uni-due.de/sjmsschl/locprivfogsim) [3] was extended to
cover computation offloading scenarios to investigate the impact of computation offloading strategies on the accuracy of
location privacy attacks. This work was done as part of an bachelor's thesis at the University Duisburg-Essen by
Markus Maximilian Schlotbohm in the year 2021.

> **_NOTE:_** The latest version of the source code can be found at https://git.uni-due.de/sjmsschl/locprivfogsim

## Getting Started

To get a local copy up and running follow these simple steps.

### Prerequisites

The following list lists the software used for this project. This software (or a comparable or higher version of the software) is required to get the simulator up and running.
* java
  ```sh
  $ java --version
  java 15.0.2 2021-01-19
  Java(TM) SE Runtime Environment (build 15.0.2+7-27)
  Java HotSpot(TM) 64-Bit Server VM (build 15.0.2+7-27, mixed mode, sharing)
  ```
* eclipse
  ```txt
  Eclipse IDE for Java Developers

  Version: Neon Release (4.6.0)
  Build id: 20160613-1800
  ```
* git

### Installation

1. Clone the repo
   ```sh
   git clone https://git.uni-due.de/sjmsschl/locprivfogsim.git
   ```
2. Import the project into your eclipse workspace by:
    - Right click in the `Project Explorer` or `Package Explorer` and select `Import...`
    - In the `Import` dialog select `Existing Projects into Workspace` under the `General` tab and click `Next >`
    - Select the root directory and browse to the location of the repo on your machine
    - Select the project in the `Projects` list. It is listed as `LocPrivFogSim`
    - Click `Finish` to import the project into your workspace

## Notes

Simulation-based analysis of threats to location privacy in fog computing.
The experiments main file is located in `src/org/fog/vmmobile/TextExample2.java`.
The extension files of LocPrivFogSim are located in `src/org/fog/privacy`.
The new extended computation offloading features are located in `src/org/fog/offloading`. To gain more information on how the simulator works read [4] located in the `docs` folder of this repo. Chapter 3 covers the implementation of Computation Offloading in LocPrivFogSim.

## Acknowledgements

Special thanks to

* Theresa Wettig
* Dr. Zoltán Ádám Mann
* Dr. Andreas Metzger
* Prof. Dr. Klaus Pohl
* Prof. Dr.-Ing. Amr Rizk

## References

* [1] H. Gupta, A. V. Dastjerdi, S. K. Ghosh, R. Buyya, "iFogSim: A toolkit for modeling and simulation of resource management techniques in the Internet of Things, Edge and Fog computing environments," Software: Practice and Experience, vol. 47, no. 9, pp. 1275-1296, 2017.
* [2] C. Puliafito, D. M. Gonçalves, M. M. Lopes, L. L. Martins, E. Madeira, E. Mingozzi, O. Rana, L. F. Bittencourt, „MobFogSim: Simulation of mobility and migration for fog computing,“ Simulation Modelling Practice and Theory, Bd. 101, Nr. 1569-190X, p. 102062, 2020.
* [3] T. Wettig, „Erweiterung des Simulators MobFogSim zur Simulation von Location-Privacy-Angriffen auf Fog-Computing-Systeme,“ Universität Duisburg-Essen, 2020.
* [4] M. Schlotbohm "Consideration of Computation Offloading Strategies in a Simulation-Based Analysis of Threats to Location Privacy in Fog Computing", Universität Duisburg-Essen, 2021.