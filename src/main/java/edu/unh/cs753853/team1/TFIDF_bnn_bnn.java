package edu.unh.cs753853.team1;

import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;


/*
 * Class to hold docId with associated score. 
 * Allows for easier sorting
 */
class DocResult {
	public int docId;
	public int score;
	
	// constructor
	public DocResult(int id, int s) {
		docId = id;
		score = s;
	}
}

/*
 * Comparator to allow PriorityQueue to sort DocResults
 */
class DocComparator implements Comparator<DocResult> {
	
	@Override
	public int compare(DocResult d1, DocResult d2) {
		if(d1.score < d2.score)
            return 1;
        if(d1.score > d2.score)
            return -1;
        return 0;
	}
}


/*
 * the indexing and querying of documents
 * uses tf-idf: bnn.bnn
 * 
 */
public class TFIDF_bnn_bnn {
	
	private IndexSearcher indexSearcher = null;
	private QueryParser queryParser = null;
	
	// query pages
	private ArrayList<Data.Page> queryPages;
	
	// num docs to return for a query
	private int numDocs = 100;
		
	// map of queries to document results with scores
	HashMap<Query, ArrayList<DocumentResults> > queryResults;
	
	
	// directory  structure..
	static final private String INDEX_DIRECTORY = "index";
	static final private String OUTPUT_DIR = "output";
	
	private String runFile = "/tfidf_bnn_bnn.run";
	
	/*
	 * @param pageList
	 * @param maxDox
	 */
	TFIDF_bnn_bnn(ArrayList<Data.Page> pageList, int maxDox) throws IOException, ParseException {
		queryPages = pageList;
		numDocs = maxDox;
		
		queryParser = new QueryParser("parabody", new StandardAnalyzer());
		
		indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));
		
		SimilarityBase bnn = new SimilarityBase() {
			protected float score(BasicStats stats, float freq, float decLen) {
				return freq > 0 ? 1 : 0;
			}
			@Override
			public String toString() {
				return null;
			}
		};
		indexSearcher.setSimilarity(bnn);
	}

	/*
	 *  method to go through and score docs for each query
	 *  @throws ParseException
	 */
	public void doScoring() throws ParseException, IOException {
		queryResults = new HashMap<>();
		
		HashMap<Query, HashMap<Integer, Integer> > results = new HashMap<>();
		
		// run through cbor.outlines for queries
		for(Data.Page page: queryPages) {
			ArrayList<TermQuery> queryTerms = new ArrayList<>();  
			
            Query qry = queryParser.parse(page.getPageName());      
            String qid = page.getPageId();
			
            
			for(String term: page.getPageName().split(" ")) {
				TermQuery cur = new TermQuery(new Term("parabody", term));
				queryTerms.add(cur);
			}
 			
			
			HashMap<Integer, Integer> docScores = new HashMap<>();
			for(TermQuery term: queryTerms) {
				TopDocs topDocs = indexSearcher.search(term, numDocs);
				for(int i = 0; i < topDocs.scoreDocs.length; i++) {
					Document doc = indexSearcher.doc(topDocs.scoreDocs[i].doc);
					
					if( !docScores.containsKey(topDocs.scoreDocs[i].doc) ) {
						docScores.put(topDocs.scoreDocs[i].doc, 1);
					}
					else {
						int prev = docScores.get(topDocs.scoreDocs[i].doc);
						docScores.put(topDocs.scoreDocs[i].doc, ++prev);
					}
					
				}
				
				
			}
			results.put(qry, docScores);
		}
		writeResults(results);
		
	}
	
	/* 
	 * 
	 */
	private void writeResults(HashMap<Query, HashMap<Integer, Integer> > map) throws IOException {
		System.out.println("TFIDF_bnn_bnn writing results to: \t\t" + OUTPUT_DIR + "/tfidf_bnn_bnn.run");
		FileWriter writer = new FileWriter(new File(OUTPUT_DIR + runFile));
		
		Set<Query> keys = map.keySet();
		Iterator<Query> iter = keys.iterator();
		while(iter.hasNext()) {
			Query curQuery = iter.next();
			HashMap<Integer, Integer> doc = map.get(curQuery); 
			String q = curQuery.toString();
			Set<Integer> tmp = doc.keySet();
			Iterator<Integer> docIds = tmp.iterator(); 
			
			PriorityQueue<DocResult> queue = new PriorityQueue<>(new DocComparator());
			while(docIds.hasNext()) {
				int curDocId = docIds.next();
				int score = doc.get(curDocId);
				DocResult tmsRes = new DocResult(curDocId, score);
				queue.add(tmsRes);
			}
			
			int count = 0;
			DocResult cur;
			while((cur = queue.poll()) != null && count++ < 100) {
//				String rank = Integer.toString(count);
				String line = cur.docId + " Q0 " + indexSearcher.doc(cur.docId).getField("paraid").stringValue() + " " + count + " " + cur.score + " " + "team1-tfidf_bnn_bnn";

				writer.write(line + '\n');
			}
		}
		writer.close();
		
	}
}
