PowerModelSquare
================

.. java:package:: org.cloudbus.cloudsim.power.models
   :noindex:

.. java:type:: public class PowerModelSquare extends PowerModelSimple

   Implements a power model where the power consumption is the square of the resource usage.

   If you are using any algorithms, policies or workload included in the power package please cite the following paper:

   ..

   * \ `Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24, Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012 <http://dx.doi.org/10.1002/cpe.1867>`_\

   :author: Anton Beloglazov, Manoel Campos da Silva Filho

Constructors
------------
PowerModelSquare
^^^^^^^^^^^^^^^^

.. java:constructor:: public PowerModelSquare(double maxPower, double staticPowerPercent)
   :outertype: PowerModelSquare

   Instantiates a new power model square.

   :param maxPower: the max power that can be consumed in Watt-Second (Ws).
   :param staticPowerPercent: the static power usage percentage between 0 and 1.

