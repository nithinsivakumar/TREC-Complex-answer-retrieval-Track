package edu.unh.cs980;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import unh.edu.cs980.RetrievalModel.BM25;

public class Main {

	public static void main(String[] args) throws IOException {
		
		System.setProperty("file.encoding", "UTF-8");

		String pagesFile = args[0];
		String indexPath = args[1];
		String outputPath = args[2];
		
		// Start searching for the passages
		BM25 bm25 = new BM25(outputPath, outputPath, outputPath);
		
		Map<String, List<String>> pageHeadingMap = bm25.getPageHeadingMap();
		Map<String, List<String>> sectionHeadingMap = bm25.getPageHeadingMap();
		
		
		

	}

}
