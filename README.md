<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/LocPrivFogSim/LocPrivFogSim">
    <img src="images/logo.png" alt="LocPrivFogSim Logo" width="80" height="80">
  </a>

  <h3 align="center">LocPrivFogSim</h3>

  <p align="center">
    A simulator to analyse threats of computation offloading strategies to location privacy in fog computing.
  </p>
</div>

## About

TODO: Abstract

## Getting Started

To get a local copy up and running follow these simple steps...

### Prerequisites

The following lists of software (or a comparable or higher version of the software) is needed in order to get this project
up and running.

* **java** or a newer compatible version
  ```sh
  $ java --version
  java 15.0.2 2021-01-19
  Java(TM) SE Runtime Environment (build 15.0.2+7-27)
  Java HotSpot(TM) 64-Bit Server VM (build 15.0.2+7-27, mixed mode, sharing)
  ```
* **IntelliJ IDEA** or any other comparable IDE or Editor of your choice
  ```txt
  IntelliJ IDEA 2021.2.3 (Ultimate Edition)

  Build IU-212.5457.46, build on October 12, 2021
  Runtime version: 11.0.12+7-b1504.40 amd64
  VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.

  Powered by open-source software
  Copyright © 2000-2021 JetBrains s.r.o.
  ```
* **git**

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/LocPrivFogSim/LocPrivFogSim.git
   ```
2. The main method is located in `src/org/fog/vmmobile/TODO.java`. To run the simulation the following program arguments need to be passed in:
   ```sh
   $ java -jar LocPrivFogSim.jar $scenario $rate $seed1 $seed2 $interval $strategy $threshold
   ```
   * **Scenario (int):** The scenario used (1 = TODO; 2 = TODO)
   * **Rate (double):** The rate of compromised devices (range from 0.0 to 1.0)
   * **Seed 1 (int):** TODO
   * **Seed 2 (int):** TODO
   * **Interval (double):** TODO
   * **Strategy (string):** The offloading strategy to use in the simulation (possible values are `"BelowThresholdRandomDevice"`, `"BelowThresholdLowestResponseTime"`).
   * **Threshold (double):** The offloading threshold below which the devices are selected by the offloading strategy.

## Usage

## References

## License

## Contact

## Acknowledgments
