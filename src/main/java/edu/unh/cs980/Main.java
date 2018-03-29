package edu.unh.cs980;

import java.io.FileInputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import edu.unh.cs980.Classifier.ClassifyPassageHeadings;
import edu.unh.cs980.RetrievalModel.BM25;
import edu.unh.cs980.TopicModel.TopicModelGenerator;
import weka.classifiers.Classifier;

public class Main {

	public static void main(String[] args) throws Exception {
		
		System.setProperty("file.encoding", "UTF-8");

		String pagesFile = args[0];
		String indexPath = args[1];
		String outputPath = args[2];
		
		// Start searching for the passages
		BM25 bm25 = new BM25(pagesFile, indexPath, outputPath);
		
		Map<String, List<String>> pageHeadingMap = bm25.getPageHeadingMap();
		Map<String, List<String>> sectionHeadingMap = bm25.getSectionHeadingMap();
		
		//TopicModelGenerator tmg = new TopicModelGenerator(pageHeadingMap, outputPath+"/trainPageHeading");
		
		System.out.println("Training Set Generated");
		
		//ClassifyPassageHeadings cpf =  new ClassifyPassageHeadings("/Users/Nithin/Desktop/Runfile/trainPageHeading");
		
		Classifier cls = (Classifier) weka.core.SerializationHelper.read("/Users/Nithin/git/TREC-Complex-answer-retrieval-Track/trainedModel/RF_Page.model");
		
		System.out.println();
		

	}

}
