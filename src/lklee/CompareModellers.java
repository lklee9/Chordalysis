package lklee;

import core.explorer.ChordalysisModeller;
import core.explorer.ChordalysisModellingSMT;
import core.model.DecomposableModel;
import lklee.explorer.ChordalysisModellingChiSquaredJT;
import lklee.generator.RandomStructureGenerator;
import loader.LoadWekaInstances;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CompareModellers {

	public static void main(String[] args) throws Exception {
		int nRuns = 20;
		int nExp = 10;
		int initNVars = 10;
		int incVar = 10;
		int initNVals = 5;
		double[] method1AvgTimes = new double[nExp];
		double[] method2AvgTimes = new double[nExp];
		for (int i = 0; i < nExp; i++) {
			int nVar = initNVars + (incVar * i);
			compareModelsOverRuns(
					i, nRuns, nVar, initNVals,
					method1AvgTimes, method2AvgTimes
			);
		}
		System.out.println("========== Avg Times ===========");
		for (int i = 0; i < nExp; i++) {
			System.out.println("nVariables: " + (initNVars + (incVar * i)));
			System.out.println("\t Method 1: " + method1AvgTimes[i]);
			System.out.println("\t Method 2: " + method2AvgTimes[i]);
		}


	}

	private static void compareModelsOverRuns (
			int experimentIdx, int nRuns,
			int nVariables, int maxNValues,
			double[] method1AvgTimes, double[] method2AvgTimes
	) throws Exception {
		Random random = new Random();
		int[] nVars = new int[nRuns];
		int[] nMaxVals = new int[nRuns];
		String[][] modelStr = new String[nRuns][2];
		long[] method1Times = new long[nRuns];
		long[] method2Times = new long[nRuns];
		for (int i = 0; i < nRuns; i++) {
			System.out.println("Run " + i);
			nVars[i] = nVariables; //random.nextInt(10) + 5;
			nMaxVals[i] = maxNValues; //random.nextInt(5) + 3;
			modelStr[i] = compareModels(
					i,
					nVars[i], nMaxVals[i],
					method1Times, method2Times
			);
			//TimeUnit.SECONDS.sleep(5);
		}
		System.out.println("============ Run Time ============");
		for (int i = 0; i < nRuns; i++) {
			System.out.println("Method 1: " + method1Times[i]);
			System.out.println("Method 2: " + method2Times[i]);
		}
		double avg1 = Arrays.stream(method1Times).skip(2).average().orElse(-1);
		method1AvgTimes[experimentIdx] = avg1;
		double avg2 = Arrays.stream(method2Times).skip(2).average().orElse(-1);
		method2AvgTimes[experimentIdx] = avg2;
		System.out.println("Average Method 1: " + avg1);
		System.out.println("Average Method 2: " + avg2);
		System.out.println("============ Not same ============");
		for (int i = 0; i < nRuns; i++) {
			if (modelStr[i] != null) {
				System.out.println("nVariables: " + nVars[i]);
				System.out.println("nMaxValues: " + nMaxVals[i]);
				System.out.println("Model 1: " + modelStr[i][0]);
				System.out.println("Model 2: " + modelStr[i][1]);
			}
		}
	}

	private static String[] compareModels(
			int runIdx,
			int nVariables, int maxNValues,
			long[] method1Times, long[] method2Times
	) throws Exception {
		String rootFolder = "./tmp/data/";
		if (!new File(rootFolder).exists()) {
			new File(rootFolder).mkdirs();
		}

		File rep = new File(rootFolder + "/");
		if (!rep.exists()) {
			rep.mkdirs();
		}

		Random rand = new Random();
		String fileName = "data_nVar=" + nVariables + "_maxValues=" + maxNValues;
		String filePath = rootFolder +"/" + fileName + ".arff";
		File arffFile = new File(filePath);
		if (!arffFile.exists()) {
			new FileOutputStream(arffFile).close();
		}
		System.out.println("Create Data");
		arffFile = genData(arffFile, nVariables, maxNValues);

		System.out.println("Loading Data");
		ArffLoader loader = new ArffLoader();
		loader.setFile(arffFile);
		Instances instances = loader.getDataSet();
		String[] variablesNames = new String[instances.numAttributes()];
		for (int i = 0; i < variablesNames.length; i++) variablesNames[i] = "n" + i;

		List<ChordalysisModeller> modellerList = new ArrayList<>();
		long startTime = 0;
		long endTime = 0;
		// Test method 1
		modellerList.add(new ChordalysisModellingChiSquaredJT(
		//modellerList.add(new ChordalysisModellingChiSquared(
		//modellerList.add(new ChordalysisModellingSMT(
				LoadWekaInstances.makeModelData(instances, true), 0.05
		));

		//modellerList.add(new ChordalysisModellingJtChiSquared(
		//modellerList.add(new ChordalysisModellingChiSquared(
		modellerList.add(new ChordalysisModellingSMT(
				LoadWekaInstances.makeModelData(instances, true), 0.05
		));
		startTime = System.currentTimeMillis();
		String a = getModelString(modellerList.get(0), variablesNames);
		endTime = System.currentTimeMillis();
		method1Times[runIdx] = endTime - startTime;
		startTime = System.currentTimeMillis();
		String b = getModelString(modellerList.get(1), variablesNames);
		endTime = System.currentTimeMillis();
		method2Times[runIdx] = endTime - startTime;
		return !a.equals(b) ? new String[]{a, b} : null;
	}

	private static String getModelString(ChordalysisModeller modeller, String[] variablesNames) {
		System.out.println("============" + modeller.toString() + "============");
		System.out.println("Learning...");
		modeller.buildModel();
		DecomposableModel bestModel = modeller.getModel();
		System.out.println("The model selected is:");
		System.out.println(bestModel.toString(variablesNames));
		//PrintableModel.display(bestModel, variablesNames);
		return bestModel.toString(variablesNames);
	}

	private static File genData(File arff, int nVariables, int maxNValues) throws Exception{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);

		// Default values
		//int nVariables = 100;
		int maxNParents = 5;
		//int maxNValues= 5;
		int nDataPoints = 50000;
		double alphaDirichlet = 10.0;
		long seed = 3071980L;

		RandomStructureGenerator gen = new RandomStructureGenerator(nVariables, maxNParents,maxNValues,nDataPoints,alphaDirichlet, seed);
		gen.generateDataset(arff);
		return arff;
	}
}
