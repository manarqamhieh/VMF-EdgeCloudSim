package edu.boun.edgecloudsim.applications.sample_app4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opencsv.CSVWriter;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;

public class BestVirtualMachineFit {

	List<TaskProperty> tmpTaskList = new ArrayList<TaskProperty>();
	List<TaskProperty> failedTasks = new ArrayList<TaskProperty>();
	List<myVM> VMList;
	private static int count = 0;
	String[] results;
	String[] vmLoadRes;
	String[] vmAvgLoad;

	public BestVirtualMachineFit() {

	}

	public BestVirtualMachineFit(double W) {
		SimSettings.W = W;
	}

	public BestVirtualMachineFit(double W, double alpha, double beta) {
		SimSettings.W = W;
		SimSettings.alpha = alpha;
		SimSettings.beta = beta;
	}

	@SuppressWarnings("resource")
	public void startAlgorithm() {

		try {

			count = 0;

			// general results
			File file = new File(
					"sim_results/ite1/BVMF_results_alpa" + SimSettings.alpha + "_beta" + SimSettings.beta + ".csv");
			FileWriter outputfile = new FileWriter(file);
			CSVWriter csvwriter = new CSVWriter(outputfile);
			String[] header = { "W", "A", "B", "#tasks", "#onMobile", "#offloaded", "#Success%", "#Failed%", "utilizedVMs", "totalVMs" };
			csvwriter.writeNext(header);

			// general results about VMs
			File file2 = new File("sim_results/ite1/BVMF_vmsLoad_detailed_alpa" + SimSettings.alpha + "_beta"
					+ SimSettings.beta + ".csv");
			FileWriter outputfile2 = new FileWriter(file2);
			CSVWriter csvwriter2 = new CSVWriter(outputfile2);
			String[] header2 = { "W", "A", "B", "#offloaded", "VM#", "VM_Type", "Rem Exec", "#assigned_tasks" };
			csvwriter2.writeNext(header2);

			File file3 = new File("sim_results/ite1/BVMF_vmsLoad.csv");
			FileWriter outputfile3 = new FileWriter(file3);
			CSVWriter csvwriter3 = new CSVWriter(outputfile3);
			String[] header3 = { "time", "loadOnEdge", "loadonCloud", "loadOnMobile" };
			csvwriter3.writeNext(header3);

			// double w = SimSettings.W;
			double stW = 6; // like other apps
			// double maxW = SimSettings.W * 300;
			
			// double incr = (maxW - w) / 10;
			//double incr = 6;
			for (; stW < SimSettings.maxW; stW += SimSettings.incr) {

				int edgeVMCount = 0, cloudVMCount = 0;
				double edgeVMExec = 0, cloudVMExec = 0;

				// SimSettings.getInstance().taskList2 should be a sorted list of tasks.
				getVMs();
				// sorting tasklist2 is done in load generator model
				// sort (decreasing) VMs based on bandwidth
				Collections.sort(VMList, new VmBandwidthComparator());

				// Required: Instead of testing code;
				// get a copy of sorted list (sorting is done in load generator model)
				tmpTaskList.clear();
				tmpTaskList.addAll(SimSettings.getInstance().taskList2);
				results = new String[10];
				vmLoadRes = new String[8];
				vmAvgLoad = new String[4];

				for (myVM vm : VMList) {
					vm.setRem(stW);
					vm.assignedTasks.clear();
				}
				// just for testing
//		taskList2.addAll(SimSettings.getInstance().taskList2.subList(0, 100));
//		double sum = 0;
//		for (TaskProperty t : taskList2) {
//			sum+=t.getExecutionTimeonMobile();
//		}
//		SimSettings.W = sum/100.0;

				int onMobile = 0, offloaded = 0, failed = 0;
				double[] remVM = new double[VMList.size()];
				failedTasks.clear();

				// loop through all tasks
				for (TaskProperty task : tmpTaskList) {

					// System.out.println(task.toString());
					Arrays.fill(remVM, stW + 1);

					// decide if the the task will be offloaded of not
					if (task.getExecutionTimeonMobile() > stW) {
						// System.out.println(" --- On mobile");
						onMobile++;
					} else { // execution time of task is <= max remaining time
						// System.out.println(" === Offloaded");
						offloaded++;

						double taskexec = 0; // dummy initialization
						for (int j = 0; j < VMList.size(); j++) {
							if (VMList.get(j).getType().equals("CloudVM")) {
								taskexec = task.getExecutionTimeonCloud();
							} else {
								taskexec = (double) task.getLength() / VMList.get(j).getMIPS();
							}
							if (VMList.get(j).getRem() >= taskexec) {
								remVM[j] = VMList.get(j).getRem();
							}
						}

						double minRemVM = stW + 1; // highest remaining time
						int minIndex = -1;
						for (int j = 0; j < remVM.length; j++) {
							if (remVM[j] < minRemVM) {

								minRemVM = remVM[j];
								minIndex = j;
							}
						}
						if (minIndex != -1) {
							if (VMList.get(minIndex).getType().equals("CloudVM")) {
								VMList.get(minIndex).updateRem(task.getExecutionTimeonCloud()); // reduce current
																								// remaining
																								// time
							} else {
								VMList.get(minIndex)
										.updateRem((double) task.getLength() / VMList.get(minIndex).getMIPS()); // reduce
																												// current
																												// remaining
																												// time

							}

							VMList.get(minIndex).assignedTasks.add(task);
						} else {
							failed++;
							failedTasks.add(task);
							// System.out.println("Failed: execution on cloud:
							// "+task.getExecutionTimeonCloud()+" - VM "+((double) task.getLength() /
							// VMList.get(2).getMIPS()));
//					System.out.println("BestVirtualMachineFit.startAlgorithm()");
//					System.out.println("VM assignment failed");
							// System.exit(0);
						}

					}
				}
				
				
				results[0] = Double.toString(stW);
				results[1] = Double.toString(SimSettings.alpha);
				results[2] = Double.toString(SimSettings.beta);
				results[3] = Double.toString(tmpTaskList.size());
				results[4] = Double.toString(onMobile);
				results[5] = Double.toString(offloaded);
				results[6] = Double.toString((double) (offloaded - failed) / offloaded * 100);
				results[7] = Double.toString((double) failed / offloaded * 100);


				// "W", "A", "B", "#offloaded", "VM#", "VM_Type", "Rem Exec", "#assigned_tasks"
				// };
				vmLoadRes[0] = results[0];
				vmLoadRes[1] = results[1];
				vmLoadRes[2] = results[2];
				vmLoadRes[3] = results[5];

				String filename = "sim_results/ite1/BVMF_W_" + stW + "_alpha" + SimSettings.alpha + "_beta"
						+ SimSettings.beta + "_" + SimSettings.getInstance().taskList2.size() + "_" + count + ".log";
				count++;

				BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

				writer.write("#onMobile = " + onMobile + "  --- #offloaded = " + offloaded + " --- failed = " + failed
						+ " === Total = " + tmpTaskList.size() + "\n");
				// printing results
				int utilizedVMs = 0;
				for (int i = 0; i < VMList.size(); i++) {
					vmLoadRes[4] = Integer.toString(i);
					vmLoadRes[5] = VMList.get(i).getType();
					vmLoadRes[6] = Double.toString(VMList.get(i).getRem());
					vmLoadRes[7] = Integer.toString(VMList.get(i).assignedTasks.size());

					if (VMList.get(i).assignedTasks.size() > 0)
						utilizedVMs++;
					if (vmLoadRes[5].equals("edgeVM")) {
						edgeVMCount++;
						edgeVMExec += (stW - VMList.get(i).getRem());
					} else if (vmLoadRes[5].equals("CloudVM")) {
						cloudVMCount++;
						cloudVMExec += (stW - VMList.get(i).getRem());
					}

					writer.write("(" + i + ") " + vmLoadRes[5] + " - remaining time = " + vmLoadRes[6]
							+ " --- #tasks = " + vmLoadRes[7] + "\n");
					for (TaskProperty d : VMList.get(i).assignedTasks) {
						double xx = (double) d.getLength() / VMList.get(i).getMIPS();
						writer.write(
								d + " --- exectimecloud = " + d.getExecutionTimeonCloud() + " exec edge  " + xx + "\n");
					}
					csvwriter2.writeNext(vmLoadRes);
					writer.write("-----------------------\n");

				}

				writer.close();

				vmAvgLoad[0] = Double.toString(stW);
				vmAvgLoad[1] = Double.toString((edgeVMExec / edgeVMCount) / stW * 100);
				vmAvgLoad[2] = Double.toString((cloudVMExec / cloudVMCount) / stW * 100);

				csvwriter3.writeNext(vmAvgLoad);

								results[8] = Integer.toString(utilizedVMs);
				results[9] = Integer.toString(edgeVMCount+cloudVMCount);
				csvwriter.writeNext(results);

				String ff = "sim_results/ite1/BVMF_failed_W_" + stW + "_alpha" + SimSettings.alpha + "_beta"
						+ SimSettings.beta + "_" + SimSettings.getInstance().taskList2.size() + "_" + count + ".log";
				BufferedWriter failedFile = new BufferedWriter(new FileWriter(ff));

				for (int f = 0; f < failedTasks.size(); f++) {
					failedFile.write(f + "-" + failedTasks.get(f));
				}
				failedFile.close();
				// System.exit(0);
			}
			csvwriter2.close();
			csvwriter.close();
			csvwriter3.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getVMs() {
		int count = 0;
		// prepare a list of all VMs. The difference is the MIPS
		// VMs on the cloud

		long maxB = 0, maxMIPS = 0;
		VMList = new ArrayList<myVM>();

		for (int i = 0; i < SimSettings.getInstance().getNumOfCloudHost(); i++) {
			for (int j = 0; j < SimSettings.getInstance().getNumOfCloudVMs(); j++) {
				VMList.add(new myVM(count, SimSettings.getInstance().getMipsForCloudVM(),
						SimSettings.getInstance().getWanBandwidth(), "CloudVM"));

				if (VMList.get(VMList.size() - 1).getBandwidth() > maxB)
					maxB = VMList.get(VMList.size() - 1).getBandwidth();
				if (VMList.get(VMList.size() - 1).getMIPS() > maxMIPS)
					maxMIPS = VMList.get(VMList.size() - 1).getMIPS();
				count++;
			}
		}

		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");

		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			Element hosts = (Element) datacenterElement.getElementsByTagName("hosts").item(0);
			NodeList hostsList = hosts.getElementsByTagName("host");

			for (int j = 0; j < hostsList.getLength(); j++) {
				Node hostNode = hostsList.item(j);
				Element hostElement = (Element) hostNode;

				Element VMs = (Element) hostElement.getElementsByTagName("VMs").item(0);
				NodeList vmList = VMs.getElementsByTagName("VM");
				for (int k = 0; k < vmList.getLength(); k++) {
					Node vmNode = vmList.item(k);
					Element vmElement = (Element) vmNode;
					long edgeVmMips = Long.parseLong(vmElement.getElementsByTagName("mips").item(0).getTextContent());
					VMList.add(new myVM(count, edgeVmMips, SimSettings.getInstance().getWlanBandwidth(), "edgeVM"));

					if (VMList.get(VMList.size() - 1).getBandwidth() > maxB)
						maxB = VMList.get(VMList.size() - 1).getBandwidth();
					if (VMList.get(VMList.size() - 1).getMIPS() > maxMIPS)
						maxMIPS = VMList.get(VMList.size() - 1).getMIPS();

					count++;

				}
			}
		}
		for (myVM myVM : VMList) {
			double val = (SimSettings.alpha * myVM.getMIPS() / maxMIPS)
					+ (SimSettings.beta * myVM.getBandwidth() / maxB);
			myVM.setP(val);
		}

		Collections.sort(VMList, new VmBandwidthComparator());

	}
}

class myVM {
	private int id;
	private long MIPS;
	private long bandwidth;
	private String type;
	List<TaskProperty> assignedTasks;
	private double rem; //
	private double p;

	public myVM(int _id, long _MIPS, long _bandwidth, String _type) {
		id = _id;
		MIPS = _MIPS;
		bandwidth = _bandwidth;
		type = _type;
		assignedTasks = new ArrayList<TaskProperty>();

		// setRem(SimSettings.W);
	}

	public int getId() {
		return id;
	}

	public long getMIPS() {
		return MIPS;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public String getType() {
		return type;
	}

	public double getExecutionTime(TaskProperty taski) {
		return ((double) taski.getInputFileSize() / bandwidth) + ((double) taski.getLength() / MIPS)
				+ ((double) taski.getOutputFileSize() / bandwidth);

	}

	@Override
	public String toString() {
		return String.format("VM {ID = %d\t MIPS = %d\t BW = %d\t type = %s \n}", id, MIPS, bandwidth, type);

	}

	public double getRem() {
		return rem;
	}

	public void setRem(double rem) {
		this.rem = rem;
	}

	public void updateRem(double rem) {
		this.rem -= rem;
	}

	public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}

}

class VmBandwidthComparator implements Comparator<myVM> {
	@Override
	public int compare(myVM a, myVM b) {
		// return a.getBandwidth() > b.getBandwidth() ? -1 : a.getBandwidth() ==
		// b.getBandwidth() ? 0 : 1;
		return a.getP() > b.getP() ? -1 : a.getP() == b.getP() ? 0 : 1;
	}
}
