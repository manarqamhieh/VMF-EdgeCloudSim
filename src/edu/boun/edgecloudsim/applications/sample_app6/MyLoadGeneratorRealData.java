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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MyLoadGeneratorRealData extends LoadGeneratorModel {
	
	private static final String COMMA_DELIMITER = null;
	int taskTypeOfDevices[];

	//List<TaskProperty> sortedTasks;
	
	public MyLoadGeneratorRealData(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() throws FileNotFoundException, IOException {

		taskList = new ArrayList<TaskProperty>();
		//sortedTasks = new ArrayList<TaskProperty>();

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

		
		
		try (BufferedReader br = new BufferedReader(new FileReader("scripts/sample_app6/data/tasks_data.csv"))) {
			String line;
			String COMMA_DELIMITER = ",";
			// some default values for active and idle periods and poissonMean
			double activePeriod = 45;
			double idlePeriod = 15;
			double poissonMean = 5;
			
			// use mips_for_mobile_vm=2340 in default_config.properties
			// double mobileMIPS = 2340; // from some benchmark found on the Internet === 2339.35
			// double serverMIPS = mobileMIPS*3; // maybe no need for that, we can achieve
			// MI on server from
			// number of instruction (MI) is supposed to be the same on server and edge
			// nodes (the difference is in the MIPS)

			br.readLine(); // discard header
			int i = 1;
			
			SimSettings.getInstance().maxI = 0;
			SimSettings.getInstance().maxD = 0;
			double sum = 0;
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

				long length = (long) (SimSettings.getInstance().getMipsForMobileVM() * Double.parseDouble(values[6]));
				
				
				//System.out.println("("+(i++)+")name: " + values[1] + " - EN time: " + values[6] + " - SER time: " + values[11]
				//		+ " - size: " + values[12] + " - length(MI): " + length+" - start time: "+virtualTime);
				// virtual time = start time of the job
				// 0 is mobile id (default value for now)
				// 0 is randomTaskType (there is no types)
				// 1 is PES (required cores)
				// virtual time = start time = 0 (no need for idle/active period if we assume each device has one task within period W
				Long fs = Long.parseLong(values[13]);
				
				
				// should be changed to a specific type with type = augmented reality
				Random r = new Random();
				int type = r.nextInt(4);
				taskList.add(new TaskProperty(0, (int)count, 0, 1, length, fs, fs));
				
				count++;
				if (count >= numberOfMobileDevices)
					break;
				// records.add(Arrays.asList(values));

			}
			
			calculateMax();
			System.out.println("MyLoadGeneratorRealData.initializeModel()");
			System.out.println("maxI "+SimSettings.getInstance().maxI);
			System.out.println("maxD "+SimSettings.getInstance().maxD);
			
			
			
			
			

			//sortedTasks.addAll(taskList);
			//Collections.sort(sortedTasks, new taskPiComparator());
//			System.out.println("\nMyLoadGeneratorRealData.initializeModel()");
//			System.out.println("tasklist:");
//			System.out.println(taskList);
//			sortTaskList();
//			System.out.println("sorted tasklist:");
//			System.out.println(taskList);
//			
			//System.out.println("sortedTasks:");
			//System.out.println(sortedTasks);
			
			
			//System.out.println("SimSettings.getInstance().maxI "+SimSettings.getInstance().maxI);
			//System.out.println("SimSettings.getInstance().maxD "+SimSettings.getInstance().maxD);
			//System.out.println("Max execution time = "+maxExecTime+" -- average exec time = "+SimSettings.getInstance().W);
			//System.exit(0);
		} catch (Exception e) {
			System.out.println("MyLoadGeneratorRealData.initializeModel()"+e);
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}

