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

package edu.boun.edgecloudsim.task_generator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class IdleActiveLoadGenerator extends LoadGeneratorModel {
	int taskTypeOfDevices[];
	private int jobCount = 0;

	public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	private void getTasksFromFile(String filename) {

		taskListnoJobs = new ArrayList<TaskProperty>();

		
		// read tasks from data files: Needed information

		// public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType,
		// int _pesNumber, long _length, long _inputFileSize, long _outputFileSize)
		// _mobileDeviceId === some unique id
		// _startTime == start time of scheduling
		// _taskType === from applications.xml
		// _pesNumber = number of required cores
		// length = average task's length (in MI)
		// _inputFileSize = data upload (KB)
		// _outputFileSize = data download (KB)

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			String COMMA_DELIMITER = ",";
			// some default values for active and idle periods and poissonMean
			double activePeriod = 45;
			double idlePeriod = 15;
			double poissonMean = 5;

			// use mips_for_mobile_vm=2340 in default_config.properties
			// double mobileMIPS = 2340; // from some benchmark found on the Internet ===
			// 2339.35
			// double serverMIPS = mobileMIPS*3; // maybe no need for that, we can achieve
			// MI on server from
			// number of instruction (MI) is supposed to be the same on server and edge
			// nodes (the difference is in the MIPS)

			br.readLine(); // discard header
			int i = 1;

			// SimSettings.getInstance().maxI = 0;
			// SimSettings.getInstance().maxD = 0;
			// double sum = 0;
			double count = 0;
			while ((line = br.readLine()) != null) {

				double activePeriodStartTime = SimUtils.getRandomDoubleNumber(SimSettings.CLIENT_ACTIVITY_START_TIME,
						SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);
				double virtualTime = activePeriodStartTime;

				String[] values = line.split(COMMA_DELIMITER);
				// values[0] = Group number (from real data / files)
				// values[1] = image name (video frame)
				// values[2] = loading time (ms) on edge node
				// values[3] = preprocessing time (ms) on edge node
				// values[4] = processing time (ms) on edge node
				// values[5] = postprocessing time (ms) on edge node
				// values[6] = total execution time (ms) on edge node
				// values[7] = loading time (ms) on server
				// values[8] = preprocessing time (ms) on server
				// values[9] = processing time (ms) on server
				// values[10] = postprocessing time (ms) on server
				// values[11] = total execution time (ms) on server
				// values[12] = file size (task's size) in KB
				// values [13] = file size (KB) rounded to the up

				// for (String dataField : values) {

				double exectime = Double.parseDouble(values[6]);

				ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
				while (virtualTime < simulationTime) {
					double interval = rng.sample();

					if (interval <= 0) {
						SimLogger.printLine("Impossible is occurred! interval is " + interval + " for device "
								+ " time " + virtualTime);
						continue;
					}
					virtualTime += interval;

					if (virtualTime > activePeriodStartTime + activePeriod) {
						activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
						virtualTime = activePeriodStartTime;
						continue;
					}
				}

				long length = (long) (SimSettings.getInstance().getMipsForMobileVM() * (Double.parseDouble(values[6])/1000.0));

				// System.out.println("("+(i++)+")name: " + values[1] + " - EN time: " +
				// values[6] + " - SER time: " + values[11]
				// + " - size: " + values[12] + " - length(MI): " + length+" - start time:
				// "+virtualTime);
				// virtual time = start time of the job
				// 0 is mobile id (default value for now)
				// 0 is randomTaskType (there is no types)
				// 1 is PES (required cores)
				// virtual time = start time = 0 (no need for idle/active period if we assume
				// each device has one task within period W
				Long fs = Long.parseLong(values[13]);

				// should be changed to a specific type with type = augmented reality
				Random r = new Random();
				int type = r.nextInt(4);
				taskListnoJobs.add(new TaskProperty(0, (int) count, 0, 1, length, fs, fs));

				count++;
//				if (count >= 10)
//					break;
				// records.add(Arrays.asList(values));

			}
		} catch (Exception e) {
			System.out.println("MyLoadGeneratorRealData.initializeModel()" + e);
		}

		// calculateMax();
		// System.out.println("MyLoadGeneratorRealData.initializeModel()");
		// System.out.println("maxI "+SimSettings.getInstance().maxI);
		// System.out.println("maxD "+SimSettings.getInstance().maxD);

	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();
		SimSettings.getInstance().taskList2.clear();

		// exponential number generator for file input size, file output size and task
		// length
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance()
				.getTaskLookUpTable().length][3];

		// create random number generator for each place (number of application types
		// (defined in applications.xml)
		for (int i = 0; i < SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
				continue;

			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]); // avg
																													// data
																													// upload
																													// (KB)
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]); // avg
																													// data
																													// download
																													// (KB)
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]); // avg
																													// task
																													// length
																													// (MI)
		}

		
		// Each mobile device utilizes an app type (task type)
		taskTypeOfDevices = new int[numberOfMobileDevices];
		
		boolean flag = true; // flag for one job of each task
		
		for (int i = 0; i < numberOfMobileDevices; i++) {
			//flag = true;

			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
			double taskTypePercentage = 0;

			for (int j = 0; j < SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0]; // usage percentage
				if (taskTypeSelector <= taskTypePercentage) {
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
																									// (sec)
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3]; // [3] active
																										// period (sec)
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4]; // [4] idle period
																									// (sec)
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(SimSettings.CLIENT_ACTIVITY_START_TIME,
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod); // active period starts shortly after the
																			// simulation started (e.g. 10 seconds)

			double virtualTime = activePeriodStartTime;

			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			while (virtualTime < simulationTime) {
				double interval = rng.sample();

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
				
				// if (flag) {
				if (jobCount<720) {
					jobCount++;
					taskListnoJobs.add(new TaskProperty(jobCount, randomTaskType, virtualTime, expRngList));
				//	flag = false;
					//taskList.add(new TaskProperty(i, randomTaskType, virtualTime, expRngList));
					
				}
				
				taskList.add(new TaskProperty(i, randomTaskType, virtualTime, expRngList));

			}
			flag = true;
			
		}
		
		
		//taskListnoJobs.addAll(taskList);
		
		//getTasksFromFile("scripts/sample_app6/data/tasks_data.csv");
		// calculate the maximum task length and the maximum upload size for all tasks /
		// jobs in the list to be used in our algorithm
		calculateMax();
		
		
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}
