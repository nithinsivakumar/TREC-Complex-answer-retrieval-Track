package edu.unh.cs980.Classifier;

/*
 * Author - Nithin


 * This is a classifier which classifies wikipedia passages into Headings.
 */

import java.util.List;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Resample;

public class ClassifierModel {

	private static final double CONFIDENCE_THRESHOLD = 0.2;// prediction
															// confidence

	public ClassifierModel(String arffFile, String modelPath) throws Exception {
		
	}

	public void buildRandomForestClassifier(String arffFile, String modelPath) throws Exception {
		
		System.out.println("Random Forest classifier running");
		RFClassifier rf = new RFClassifier(arffFile, modelPath);
		System.out.println("Random Forest Classifier  model built");
	}

	public void buildNaiveBayesClassifier(String arffFile, String modelPath) throws Exception {
		System.out.println("Naive Bayes classifier running");
		NaiveBayes nb = new NaiveBayes(arffFile, modelPath);
		System.out.println("Naive Bayes Classifier  model built");

	}

	public void buildJ48Classifier(String arffFile, String modelPath) throws Exception {
		System.out.println("J48 model buliding");
		J48Classifier j48 = new J48Classifier(arffFile, modelPath);
		System.out.println("J48 model built");
	}

}