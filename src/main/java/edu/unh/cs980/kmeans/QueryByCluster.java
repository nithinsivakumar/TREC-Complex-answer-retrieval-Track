package edu.unh.cs980.kmeans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryByCluster {
	
	/*
	 * the MAP will be used for classifier
	*/
	private static Map<String, List<String>> pageid_with_para_text = new HashMap<String, List<String>>();
	
	public QueryByCluster(String action, String outline_cbor, String corpus_index, String type_of_clu, String clu_index, String output_Dir) throws IOException{
		
		
		String act = action;
		String pagesFile = outline_cbor;
		String corpusPath = corpus_index;
		String flag = type_of_clu;
        String clustersPath = clu_index;
        String outputPath = output_Dir;
        
        String outputfile;
        File runfile;
		
        if(flag == "-c") {
        		outputfile = outputPath + "/runfile_cluster_types_" + action;
        		runfile = new File(outputfile);
        }else {
        		outputfile = outputPath + "/runfile_cluster_kmeans_" + action;
        		runfile = new File(outputfile);
        }
    		
        runfile.createNewFile();
		FileWriter writer = new FileWriter(runfile);		
        
        //paragraphs-run-pages
    		IndexSearcher searcher = setupIndexSearcher(corpusPath, "paragraph.lucene.vectors");
        searcher.setSimilarity(new BM25Similarity());
        final MyQueryBuilder queryBuilder = new MyQueryBuilder(new StandardAnalyzer());
        final FileInputStream fileInputStream = new FileInputStream(new File(pagesFile));
        
        int num_of_query = 0;
        
        int num_of_reRank = 0;
        
        if(act.equals("section")) {
        	
        		System.out.println("start searching for sections by using clusters...");
        	
            for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream)) {
                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
            		//"classifier" use
            		List<String> para_list = new ArrayList<String>();
            	
            		System.out.println("processing query no." + (num_of_query + 1));
            	
            		final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);

                String queryStr = buildSectionQueryStr(page, sectionPath);
                
                String[] query_clu_rank = getCluRankforStr(queryStr, clustersPath);
                
                TopDocs tops = searcher.search(queryBuilder.toQuery(queryStr), 100);
                ScoreDoc[] scoreDoc = tops.scoreDocs;
                if(!isAllNull(query_clu_rank)) {
                		//do re_rank
                		Map<String, Float> runfile_map = new HashMap<String, Float>();
                		//map para_id to para_content for "classifier" use
                		Map<String, String> para_map = new HashMap<String, String>();
                		for (int i = 0; i < scoreDoc.length; i++) {
                            ScoreDoc score = scoreDoc[i];
                            final Document doc = searcher.doc(score.doc); // to access stored content
                            // print score and internal docid
                            final String paragraphid = doc.getField("paragraphid").stringValue();
                            final String para_text = doc.getField("text").stringValue();
                            para_map.put(paragraphid, para_text);
                            final float searchScore = score.score;
                            //final int searchRank = i+1;
                            String para_indicator;
                            if(flag == "-c") {
                            		para_indicator = getDBEntities(para_text);               
                            }else {
                            		para_indicator = para_text;
                            }
                            String[] para_clu_rank = getCluRankforStr(para_indicator, clustersPath);
                            //printArray(para_clu_rank);
                            final float newScore = getScore(searchScore, query_clu_rank, para_clu_rank);
                            //System.out.println(queryId+" Q0 "+paragraphid+" "+searchRank + " "+searchScore+" Lucene-BM25");
                            //System.out.println(i);
                            runfile_map.put(paragraphid, newScore);
                    }
                		//System.out.println(para_map);
                		runfile_map = sortByComparator(runfile_map, false);
                		//printMap(runfile_map);
                		int searchRank = 0;
                		for (Map.Entry<String, Float> entry : runfile_map.entrySet()) {
                			  searchRank ++;
              			  String key = entry.getKey();
              			  float value = entry.getValue();
              			  //System.out.println("key is " + key + "; value is " + value);
              			  writer.write(queryId + " Q0 " + key + " " + searchRank + " " + value + " Lucene-BM25\n");  
              			  //for "classifier" use
              			  String para_content = para_map.get(key);
              			  //System.out.println("para content is " + para_content);
              			  para_list.add(para_content);
              		}
                		//System.out.println("tag 1 : " + para_list);
                		num_of_reRank ++;
                }else {
                	    //without re_rank
                		for (int i = 0; i < scoreDoc.length; i++) {
                        ScoreDoc score = scoreDoc[i];
                        final Document doc = searcher.doc(score.doc); // to access stored content
                        final String paragraphid = doc.getField("paragraphid").stringValue();
                        final String para_text = doc.getField("text").stringValue();
                        final float searchScore = score.score;
                        final int searchRank = i+1;
                        //System.out.println(queryId+" Q0 "+paragraphid+" "+searchRank + " "+searchScore+" Lucene-BM25");
                        //System.out.println(".");
                        writer.write(queryId + " Q0 " + paragraphid + " " + searchRank + " " + searchScore + " Lucene-BM25\n");
                        //for "classifier" use
        			  		para_list.add(para_text);
                		}
                		//System.out.println("tag 2 : " + para_list);
                	
                }
                
                pageid_with_para_text.put(queryId, para_list);
                
                num_of_query ++;

            
                }
            }
        	
        	
        }else if(act.equals("page")){
        	
        	System.out.println("start searching for pages by using clusters...");
        	
        	for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream)) {
            	
        		//"classifier" use
        		List<String> para_list = new ArrayList<String>();
        	
        		System.out.println("processing query no." + (num_of_query + 1));
        	
            final String queryId = page.getPageId();

            String queryStr = buildSectionQueryStr(page, Collections.<Data.Section>emptyList());
            
            String[] query_clu_rank = getCluRankforStr(queryStr, clustersPath);
            
            TopDocs tops = searcher.search(queryBuilder.toQuery(queryStr), 100);
            ScoreDoc[] scoreDoc = tops.scoreDocs;
            if(!isAllNull(query_clu_rank)) {
            		//do re_rank
            		Map<String, Float> runfile_map = new HashMap<String, Float>();
            		//map para_id to para_content for "classifier" use
            		Map<String, String> para_map = new HashMap<String, String>();
            		for (int i = 0; i < scoreDoc.length; i++) {
                        ScoreDoc score = scoreDoc[i];
                        final Document doc = searcher.doc(score.doc); // to access stored content
                        // print score and internal docid
                        final String paragraphid = doc.getField("paragraphid").stringValue();
                        final String para_text = doc.getField("text").stringValue();
                        para_map.put(paragraphid, para_text);
                        final float searchScore = score.score;
                        //final int searchRank = i+1;
                        String para_indicator;
                        if(flag == "-c") {
                        		para_indicator = getDBEntities(para_text);               
                        }else {
                        		para_indicator = para_text;
                        }
                        String[] para_clu_rank = getCluRankforStr(para_indicator, clustersPath);
                        //printArray(para_clu_rank);
                        final float newScore = getScore(searchScore, query_clu_rank, para_clu_rank);
                        
                        if(searchScore != newScore) {
                        		//System.out.println("re_score for " + paragraphid);
                        		//System.out.println("old score is " + searchScore + " new score is " + newScore);
                        		break;
                        }else {
                        		//System.out.println("newScore = oldScore");
                        }
                        
                        //System.out.println(queryId+" Q0 "+paragraphid+" "+searchRank + " "+searchScore+" Lucene-BM25");
                        //System.out.println(i);
                        runfile_map.put(paragraphid, newScore);
                }
            		//System.out.println("old map is");
            		//printMap(runfile_map);
            		runfile_map = sortByComparator(runfile_map, false);
            		//System.out.println("new map is");
            		//printMap(runfile_map);
            		int searchRank = 0;
            		for (Map.Entry<String, Float> entry : runfile_map.entrySet()) {
            			  searchRank ++;
          			  String key = entry.getKey();
          			  float value = entry.getValue();
          			  //System.out.println("key is " + key + "; value is " + value);
          			  writer.write(queryId + " Q0 " + key + " " + searchRank + " " + value + " Lucene-BM25\n");  
          			  //for "classifier" use
          			  String para_content = para_map.get(key);
          			  //System.out.println("para content is " + para_content);
          			  para_list.add(para_content);
          		}
            		
            		//System.out.println("tag 1 : " + para_list);
            		num_of_reRank ++;
            }else {
            	    //without re_rank
            		for (int i = 0; i < scoreDoc.length; i++) {
                    ScoreDoc score = scoreDoc[i];
                    final Document doc = searcher.doc(score.doc); // to access stored content
                    final String paragraphid = doc.getField("paragraphid").stringValue();
                    final String para_text = doc.getField("text").stringValue();
                    final float searchScore = score.score;
                    final int searchRank = i+1;
                    //System.out.println(queryId+" Q0 "+paragraphid+" "+searchRank + " "+searchScore+" Lucene-BM25");
                    //System.out.println(".");
                    writer.write(queryId + " Q0 " + paragraphid + " " + searchRank + " " + searchScore + " Lucene-BM25\n");
                    //for "classifier" use
    			  		para_list.add(para_text);
            		}
            		//System.out.println("tag 2 : " + para_list);
            	
            }
            
            pageid_with_para_text.put(queryId, para_list);
            
            num_of_query ++;

        }
        	
        		
        	
        }else {
        	
        		System.out.println("invalid action\naction is either [page] or [section]");
        		System.exit(0);
        }
        
        /*
         * section query takes much long time than page query
         */
        
        
                
        		writer.flush();//why flush?
			writer.close();
			
			stripDuplicatesFromFile(outputfile);
			
			System.out.println("Number of query is " + num_of_query);
			System.out.println("Number of re_ranked query is " + num_of_reRank);
			System.out.println("Search Done");
			
			
	}
	
	public static Map<String, List<String>> getModel(){
		return pageid_with_para_text;
	}
	
	private static void printMap(Map<String, Float> map) {		
		for (Map.Entry<String, Float> entry : map.entrySet()) {
			  String key = entry.getKey();
			  float value = entry.getValue();
			  System.out.println("key is " + key + "; value is " + value); 
		}		
	}
	
	private static Map<String, Float> sortByComparator(Map<String, Float> unsortMap, final boolean order){

        List<Entry<String, Float>> list = new LinkedList<Entry<String, Float>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Float>>()
        {
            public int compare(Entry<String, Float> o1,
                    Entry<String, Float> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Float> sortedMap = new LinkedHashMap<String, Float>();
        for (Entry<String, Float> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	private static void printArray(String[] str) {
        for(int i = 0; i < str.length; i ++) {
        		System.out.println( str[i] );
        }
        System.out.println("---");
	}
	
	private static boolean isAllNull(String[] str) {
		boolean allNull = true;
		for(int i = 0; i < str.length; i ++) {
			if(str[i] != null) {
				allNull = false;
			}
		}
		return allNull;
	}
	
	private static float getScore(float score, String[] query_clu_rank, String[] para_clu_rank) {
		//System.out.println("query_clu_rank is ");
		//printArray(query_clu_rank);
		//System.out.println("para_clu_rank is ");
		//printArray(para_clu_rank);
		float s = 0;
		String para_clu = para_clu_rank[0];
		if(para_clu != null) {
			int p = 0;
			for(int i = 0; i < query_clu_rank.length; i ++) {
				if(para_clu == query_clu_rank[i]) {
					p = i;
					break;
				}
			}
			if(p != 0){
				s = 1/p;
				//System.out.println("plus score : " + s);
			}else {
				s = -(score/2);
				//System.out.println("minus score : " + s);
			}
		}
		return s + score;
	}
	
	private static String[] getCluRankforStr(String str, String cluPath) throws IOException {
		if(str.length() > 1024) {
			str = str.substring(0, 1024);
		}
		String[] cluRank = new String[3];
		IndexSearcher searcher = setupIndexSearcher(cluPath, "entites.cluster.lucene.index");
        searcher.setSimilarity(new BM25Similarity());
        final MyQueryBuilder queryBuilder = new MyQueryBuilder(new StandardAnalyzer());
        TopDocs tops = searcher.search(queryBuilder.toQuery(str), 3);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        for (int i = 0; i < scoreDoc.length; i++) {
            ScoreDoc score = scoreDoc[i];
            final Document doc = searcher.doc(score.doc); // to access stored content
            // print score and internal docid
            final String clu_id = doc.getField("clusterid").stringValue();
            cluRank[i] = clu_id;
        }
		return cluRank;
	}
	
	public static String getDBEntities(String str) {
		String spotlightAPIurl = "http://model.dbpedia-spotlight.org/en/annotate?";
		String httpUrl = spotlightAPIurl + "text=" + str.replaceAll("[^A-Za-z0-9]", "%20");
		String responseStr = getHttpResponse(httpUrl);
		Pattern pattern = Pattern.compile("http://dbpedia.org/resource/(.*?)\",\"@support");
		Matcher matcher = pattern.matcher(responseStr);
		String newStr = "";
		while (matcher.find()) {	
			//System.out.println(matcher.group(1));
			//System.out.println(matcher.group(1).replaceAll("[^A-Za-z0-9]", ""));
			newStr += matcher.group(1) + " ";
		}   
		return newStr.replaceAll("_", " ");
	}
	
	private static String getHttpResponse(String urlStr) {
		try {

			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			conn.setRequestProperty("Accept", "application/json");
			// conn.setReadTimeout(httpRequest_timeout);
			if (conn.getResponseCode() != 200) {
				System.out.println("Failed to connect to " + urlStr + " with HTTP error code: " + conn.getResponseCode());
				if (conn.getResponseCode() == 401) {
				}
				return null;
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output = "";// br.readLine();
			String line = "";
			while ((line = br.readLine()) != null) {
				output += line;
			}

			conn.disconnect();
			return output;
		} catch (Exception e) {
			return null;
		}
	}
	
		//Author: Laura dietz
		static class MyQueryBuilder {

	        private final StandardAnalyzer analyzer;
	        private List<String> tokens;

	        public MyQueryBuilder(StandardAnalyzer standardAnalyzer){
	            analyzer = standardAnalyzer;
	            tokens = new ArrayList<>(128);
	        }

	        public BooleanQuery toQuery(String queryStr) throws IOException {

	            TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(queryStr));
	            tokenStream.reset();
	            tokens.clear();
	            while (tokenStream.incrementToken()) {
	                final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
	                tokens.add(token);
	            }
	            tokenStream.end();
	            tokenStream.close();
	            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	            for (String token : tokens) {
	                booleanQuery.add(new TermQuery(new Term("text", token)), BooleanClause.Occur.SHOULD);
	            }
	            return booleanQuery.build();
	        }
	    }
		
		private static IndexSearcher setupIndexSearcher(String indexPath, String typeIndex) throws IOException {
	        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
	        Directory indexDir = FSDirectory.open(path);
	        IndexReader reader = DirectoryReader.open(indexDir);
	        return new IndexSearcher(reader);
	    }
		
		private static String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
	        StringBuilder queryStr = new StringBuilder();
	        queryStr.append(page.getPageName());
	        for (Data.Section section: sectionPath) {
	            queryStr.append(" ").append(section.getHeading());
	        }
	        //System.out.println("queryStr = " + queryStr);
	        return queryStr.toString();
	    }
		
		// Remove Duplicates from the runfile for sections
		public static void stripDuplicatesFromFile(String filename) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			Set<String> lines = new HashSet<String>(); // maybe should be bigger
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
			System.out.println("Removing Duplicates");
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (String unique : lines) {
				writer.write(unique);
				writer.newLine();
			}
			writer.close();
			System.out.println("Duplicates Removed");
		}

}
