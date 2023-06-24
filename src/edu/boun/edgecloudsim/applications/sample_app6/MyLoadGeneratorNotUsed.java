/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.sample_app6;

import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MyLoadGeneratorNotUsed extends LoadGeneratorModel {
	int taskTypeOfDevices[];

	public MyLoadGeneratorNotUsed(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {

		taskList = new ArrayList<TaskProperty>();

		// TaskProperty:: startTime, mobileDeviceId, taskType, pesNumber, length,
		// outputFileSize, inputFileSize

		// exponential number generator for file input size, file output size and task
		// length
		// tasklookuptable --- from applications.xml. default file contains 4 types of
		// tasks with parameters for each one.
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance()
				.getTaskLookUpTable().length][3];

		// create random number generator for each place (number of application types
		// (defined in applications.xml)
		for (int i = 0; i < SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			// check the usage percentage of each application in applications.xml
			if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
				continue;

			// average data upload size in KB (==filesize in cloudsim) means the program's
			// size + input data
			// before execution (all has to be sent to server/used to calculate bandwidth's
			// cost)
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			// average data download size in KB (==outputsize in cloudsim) means the data
			// produced as result of cloudlet execution that needs to be transferred thought
			// the network to simulate sending response data to the user
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			// average task length (MI) = the execution length of this Cloudlet (in Million
			// Instructions (MI)) that will be executed in each defined PE (CPU core on server)
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}

		// Each mobile device utilizes an app type (task type)

		taskTypeOfDevices = new int[numberOfMobileDevices];
		for (int i = 0; i < numberOfMobileDevices; i++) {
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
			double taskTypePercentage = 0;

			// the logic is clear, but the motivation is not!! why didn't they use random
			// choice of types [0-3] ??
			

			for (int j = 0; j < SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0]; // usage percentage

				if (taskTypePercentage >= taskTypeSelector) {
					randomTaskType = j;

					break;
				}
			}
			if (randomTaskType == -1) {
				SimLogger.printLine("Impossible is occurred! no random task type!");
				continue;
			}

			taskTypeOfDevices[i] = randomTaskType;

			double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2]; // [2] poisson mean
																									// (interarrival)
																									// (sec)
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3]; // [3] active
																										// period (sec)
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4]; // [4] idle period

			// (sec)
			// CLIENT_ACTIVITY_START_TIME is a static value defined in SimSettings
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(SimSettings.CLIENT_ACTIVITY_START_TIME,
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod); // active period starts shortly after the
																			// simulation started (e.g. 10 seconds)
			// System.out.println("---- activePeriod: "+activePeriod+" ----
			// activePeriodStartTime : "+activePeriodStartTime);

			// System.out.println("--- client activity start time: "+activePeriodStartTime+"
			// --- +active period: "+activePeriod);

			double virtualTime = activePeriodStartTime;

			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			while (virtualTime < simulationTime) {
				double interval = rng.sample();

				// System.out.println("----- IdleActiveLoad ----- "+virtualTime+" /
				// "+simulationTime+" ("+interval+"/mean = "+poissonMean+")");

				if (interval <= 0) {
					SimLogger.printLine("Impossible is occurred! interval is " + interval + " for device " + i
							+ " time " + virtualTime);
					continue;
				}
				// SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + "
				// time ");
				virtualTime += interval;

				if (virtualTime > activePeriodStartTime + activePeriod) {
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					continue;
				}

				// System.out.println("\n--- mobiledeviceid "+i+" ---- taskType:
				// "+randomTaskType+" ---- starttime: "+virtualTime);
				taskList.add(new TaskProperty(i, randomTaskType, virtualTime, expRngList));
				// public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType,
				// int _pesNumber, long _length, long _inputFileSize, long _outputFileSize)
			}
			// System.out.println("--- total: task added : "+taskList.size()+"----- "+i+" /
			// numberOfMobileDevices "+numberOfMobileDevices);
		}
		// System.out.println("--- total: task added : "+taskList.size());
		// for (int j : taskTypeOfDevices) {
		// System.out.print(j+" \t");
		// }
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}
