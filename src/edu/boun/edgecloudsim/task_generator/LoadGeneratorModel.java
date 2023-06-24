/*
 * Title:        EdgeCloudSim - Load Generator Model
 * 
 * Description: 
 * LoadGeneratorModel is an abstract class which is used for 
 * deciding task generation pattern via a task list. For those who
 * wants to add a custom Load Generator Model to EdgeCloudSim should
 * extend this class and provide a concrete instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.task_generator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import weka.gui.TaskLogger;

public abstract class LoadGeneratorModel {
	protected List<TaskProperty> taskList;
	protected List<TaskProperty> taskListnoJobs;
	protected int numberOfMobileDevices;
	protected double simulationTime;
	protected String simScenario;

	public LoadGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		numberOfMobileDevices = _numberOfMobileDevices;
		simulationTime = _simulationTime;
		simScenario = _simScenario;
		taskListnoJobs = new ArrayList<TaskProperty>();

	};

	/*
	 * Default Constructor: Creates an empty LoadGeneratorModel
	 */
	public LoadGeneratorModel() {
	}

	/*
	 * each task has a virtual start time it will be used while generating task
	 */
	public List<TaskProperty> getTaskList() {
		return taskList;
	}

	/*
	 * fill task list according to related task generation model
	 */
	public abstract void initializeModel() throws FileNotFoundException, IOException;

	/*
	 * returns the task type (index) that the mobile device uses
	 */
	public abstract int getTaskTypeOfDevice(int deviceId);

	// calculate the maximum length (MI) and the maximum upload file size (KB) for
	// each task
	// accordingly, set the value of Pi for each task in the list
	public void calculateMax() {

		if (taskListnoJobs != null) {
			if (!taskListnoJobs.isEmpty()) {
				SimSettings.maxI = 0;
				SimSettings.maxD = 0;
				long length, fileSize;
				double sum = 0;
				int count = 0;
				for (TaskProperty task : taskListnoJobs) {
					sum += task.getExecutionTimeonMobile();

					count++;
					length = task.getLength();
					if (length > SimSettings.maxI)
						SimSettings.maxI = length;

					fileSize = task.getInputFileSize();

					if (fileSize > SimSettings.maxD)
						SimSettings.maxD = fileSize;
				}

				SimSettings.W = sum / count;
				int x = 0;
				for (TaskProperty task : taskListnoJobs) {
					if (task.getMobileDeviceId() == x) {
						task.setPi((SimSettings.alpha * task.getLength() / SimSettings.maxI)
								+ (SimSettings.beta * task.getInputFileSize() / SimSettings.maxD));
						x++;
					}
				}

				SimSettings.getInstance().taskList2.addAll(taskListnoJobs);
				sortTaskList(SimSettings.getInstance().taskList2);
				// System.out.println("************* "+taskListnoJobs.size()+"
				// *********************** "+SimSettings.getInstance().taskList2.size());

				taskListnoJobs.clear();
				
				int i = 1;
				for (TaskProperty t : SimSettings.getInstance().taskList2) {
					SimSettings.LB += (t.getLength() / SimSettings.getInstance().getMipsForCloudVM())
							+ (t.getInputFileSize()
									/ SimSettings.getInstance().getWlanBandwidth());
					System.out.println(i+" -- "+(t.getLength() / SimSettings.getInstance().getMipsForCloudVM()));
					i++;
				}

			} else {
				SimLogger.printLine("Tasklist is empty (load generator model)!");
				System.exit(0);
			}
		} else {
			SimLogger.printLine("Tasklist is null (load generator model)!");
			System.exit(0);
		}

	}

	public void sortTaskList(List<TaskProperty> list) {
		Collections.sort(list, new taskPiComparator());
	}

}

class taskPiComparator implements Comparator<TaskProperty> {
	@Override
	public int compare(TaskProperty a, TaskProperty b) {
		return a.getPi() > b.getPi() ? -1 : a.getPi() == b.getPi() ? 0 : 1;
	}
}
