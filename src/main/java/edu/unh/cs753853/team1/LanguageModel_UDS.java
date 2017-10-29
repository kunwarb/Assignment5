package edu.unh.cs753853.team1;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

/**
 * Encapsulates the idea of the run file string and splits it into separate parts.
 * Written by Austin. I'm just borrowing it since we should stay consistent.
 * @author amf2015
 */
class RunFileString {
    public String queryId;
    public String paraId;
    public int rank;
    public float score;
    public String teamName;
    public String methodName;

    /**
     * Creates the default runfile string. For this implementation, teamName and methodName
     *  are fixed at "team1" and "DS-Dirichlet" respectively.
     */
    RunFileString()
    {
        queryId = "";
        paraId = "";
        rank = 0;
        score = 0.0f;
        teamName = "team1";
        methodName = "DS-Dirichlet";
    }

    /**
     * Create a runfile string with the given properties. teamName and methodName left out because
     *  they will be similar across the board for this program.
     *
     * @param qid   The Page Name used as the queryId
     * @param pid   The paragraph Id
     * @param r     The rank of the document
     * @param s     The score of the document
     */
    RunFileString(String qid, String pid, int r, float s)
    {
        queryId = qid;
        paraId = pid;
        rank = r;
        score = s;
        teamName = "team1";
        methodName = "DS-Dirichlet";
    }

    /**
     * Convert all the components into the final runfile string for the document
     *
     * @return String
     */
    public String toString()
    {
        return (queryId + " Q0 " + paraId + " " + rank + " " + score + " " + teamName + "-" + methodName);
    }
}


// class for LM UDS
public class LanguageModel_UDS {
    static final private String INDEX_DIRECTORY = "index";
    static final private String OUTPUT_DIRECTORY = "output"; 
    static final private String outputName = "results_uds.run";
    private final int numDocs = 100;

    /*
     * returns IndexReader at path
     */
    private static IndexReader getInedexReader(String path) throws IOException {
        return DirectoryReader.open(FSDirectory.open((new File(path).toPath())));
    } 
    /*
     * new SimilarityBase for customize scoring.
     */
    public static SimilarityBase getSimilarity() throws IOException {
        SimilarityBase sim = new SimilarityBase() {
			@Override
			protected float score(BasicStats stats, float freq, float arg2) {
				float totalTF = stats.getTotalTermFreq();
				return (freq + 1000)/(totalTF + 1000);
			}
			@Override
			public String toString() {
				return null;
			}
        };
        return sim;
    }
    
    /*
     * Constructor for the UDS implementation.
     */
    LanguageModel_UDS(ArrayList<Data.Page> pagelist) {

        try {
            ArrayList<RunFileString> resultLines = new ArrayList<>();
            for(Data.Page page : pagelist) {
                String queryStr = page.getPageId();
                ArrayList<RunFileString> res = getRanked(queryStr);
                resultLines.addAll(res);
            }
            writeArrayToFile(resultLines);
            
        } catch(java.io.IOException e) {
            e.printStackTrace();
        }
    }
    
    /*
     * gets result array of size 100 for a given query.
     */
    private ArrayList<RunFileString> getRanked(String query) throws IOException {
        IndexSearcher indexSearcher = new IndexSearcher(getInedexReader(INDEX_DIRECTORY));
        indexSearcher.setSimilarity(getSimilarity());
        
        QueryParser parser = new QueryParser("parabody", new StandardAnalyzer());
        
        ArrayList<RunFileString> ret = new ArrayList<>();
        
        try {
			Query q = parser.parse(query);
			TopDocs topDocs = indexSearcher.search(q, numDocs);
			ScoreDoc[] hits = topDocs.scoreDocs;
			for(int i = 0; i < hits.length; i++) {
				Document doc = indexSearcher.doc(hits[i].doc);
				String docId = doc.get("paraid");
				float score = hits[i].score;

				if(docId.equals("3062422698c70396fe4505a60c680d50022a3314"))
                {
                    System.out.println("UDS");
                    System.out.println(doc.get("parabody"));
                }
				
				RunFileString tmp = new RunFileString(query, docId, i, score);
				ret.add(tmp);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
        
        return ret;
    }
    
    /*
     * makes output directory if it doesn't exist. Same for output file.
     * writes the contents of an ArrayList<String> to the output file.
     */
    private void writeArrayToFile(ArrayList<RunFileString> list) throws IOException {
    		File dir = new File(OUTPUT_DIRECTORY);
    		if(!dir.exists()) {
    			if(dir.mkdir()) {
    				System.out.println("output directory made...");
    			}
    		}
    		File file = new File(OUTPUT_DIRECTORY + "/" + outputName);
    		if(file.createNewFile()) {
    			System.out.println(outputName + " file made...");
    		}
    		BufferedWriter buff = new BufferedWriter(new FileWriter(file));
    		for(RunFileString line: list) {
    			buff.write(line.toString() + "\n");
    		}
    		buff.close();
    }
}