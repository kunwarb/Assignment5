package edu.unh.cs753853.team1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccartool.Data;
import edu.unh.cs.treccartool.read_data.DeserializeData;
import edu.unh.cs.treccartool.read_data.DeserializeData.RuntimeCborException;

public class QueryParagraphs {

	private IndexSearcher is = null;
	private QueryParser qp = null;
	private boolean customScore = false;

	// directory structure..
	static final private String INDEX_DIRECTORY = "index";
	static final private String Cbor_FILE = "test200.cbor/train.test200.cbor.paragraphs";
	static final private String Cbor_OUTLINE = "test200.cbor/train.test200.cbor.outlines";
	static final private String OUTPUT_DIR = "output";

	private void indexAllParagraphs() throws CborException, IOException {
		Directory indexdir = FSDirectory.open((new File(INDEX_DIRECTORY))
				.toPath());
		IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(indexdir, conf);
		for (Data.Paragraph p : DeserializeData
				.iterableParagraphs(new FileInputStream(new File(Cbor_FILE)))) {
			this.indexPara(iw, p);
		}
		iw.close();
	}

	private void indexPara(IndexWriter iw, Data.Paragraph para)
			throws IOException {
		Document paradoc = new Document();
		paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
		paradoc.add(new TextField("parabody", para.getTextOnly(),
				Field.Store.YES));
		FieldType indexType = new FieldType();
		indexType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		indexType.setStored(true);
		indexType.setStoreTermVectors(true);

		paradoc.add(new Field("content", para.getTextOnly(), indexType));

		iw.addDocument(paradoc);
	}

	private void customScore(boolean custom) throws IOException {
		customScore = custom;
	}

	/**
	 * 
	 * @param page
	 * @param n
	 * @param filename
	 * @throws IOException
	 * @throws ParseException
	 */
	private void rankParas(Data.Page page, int n, String filename)
			throws IOException, ParseException {
		if (is == null) {
			is = new IndexSearcher(DirectoryReader.open(FSDirectory
					.open((new File(INDEX_DIRECTORY).toPath()))));
		}

		if (customScore) {
			SimilarityBase mySimiliarity = new SimilarityBase() {
				protected float score(BasicStats stats, float freq, float docLen) {
					return freq;
				}

				@Override
				public String toString() {
					return null;
				}
			};
			is.setSimilarity(mySimiliarity);
		}

		if (qp == null) {
			qp = new QueryParser("parabody", new StandardAnalyzer());
		}

		Query q;
		TopDocs tds;
		ScoreDoc[] retDocs;

		System.out.println("Query: " + page.getPageName());
		q = qp.parse(page.getPageName());

		tds = is.search(q, n);
		retDocs = tds.scoreDocs;
		Document d;
		ArrayList<String> runStringsForPage = new ArrayList<String>();
		String method = "lucene-score";
		if (customScore)
			method = "custom-score";
		for (int i = 0; i < retDocs.length; i++) {
			d = is.doc(retDocs[i].doc);
			System.out.println("Doc " + i);
			System.out.println("Score " + tds.scoreDocs[i].score);
			System.out.println(d.getField("paraid").stringValue());
			System.out.println(d.getField("parabody").stringValue() + "\n");

			// runFile string format $queryId Q0 $paragraphId $rank $score
			// $teamname-$methodname
			String runFileString = page.getPageId() + " Q0 "
					+ d.getField("paraid").stringValue() + " " + i + " "
					+ tds.scoreDocs[i].score + " team1-" + method;
			runStringsForPage.add(runFileString);
		}

		FileWriter fw = new FileWriter(QueryParagraphs.OUTPUT_DIR + "/"
				+ filename, true);
		for (String runString : runStringsForPage)
			fw.write(runString + "\n");
		fw.close();
	}

	private ArrayList<Data.Page> getPageListFromPath(String path) {
		ArrayList<Data.Page> pageList = new ArrayList<Data.Page>();
		try {
			FileInputStream fis = new FileInputStream(new File(path));
			for (Data.Page page : DeserializeData.iterableAnnotations(fis)) {
				pageList.add(page);
				System.out.println(page.toString());

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeCborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageList;
	}

	// Function to read run file and store in hashmap inside HashMap
	public static HashMap<String, HashMap<String, String>> read_dataFile(
			String file_name) {
		HashMap<String, HashMap<String, String>> query = new HashMap<String, HashMap<String, String>>();

		File f = new File(file_name);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			ArrayList<String> al = new ArrayList<>();
			String text = null;
			while ((text = br.readLine()) != null) {
				String queryId = text.split(" ")[0];
				String paraID = text.split(" ")[2];
				String rank = text.split(" ")[3];

				if (al.contains(queryId))
					query.get(queryId).put(paraID, rank);
				else {
					HashMap<String, String> docs = new HashMap<String, String>();
					docs.put(paraID, rank);
					query.put(queryId, docs);
					al.add(queryId);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (br != null)
				br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return query;
	}

	// Function to compute the Spearman_correlation_coefficient
	public static void calculateCorrelation(
			HashMap<String, HashMap<String, String>> lucene_data,
			HashMap<String, HashMap<String, String>> TFIDF_data) {
		float SpearMan_rank_correlation = (float) 0.0;
		float d = 0, d_square = 0, rank_correlation = (float) 0.0;

		for (String q : lucene_data.keySet()) {
			HashMap<String, String> luceneRanks, customRanks;
			if (TFIDF_data.keySet().contains(q)) {
				luceneRanks = lucene_data.get(q);
				customRanks = TFIDF_data.get(q);
                  int missingFile = 0;
				int n = luceneRanks.size();
				if (n == 1) {
					n = 2;
				}
				for (String key : luceneRanks.keySet()) {
					int num1 = Integer.parseInt(luceneRanks.get(key));
					if (customRanks.containsKey(key)) {
						int num2 = Integer.parseInt(customRanks.get(key));

						d = Math.abs(num1 - num2);
						d_square += (d * d);
					} else {
						missingFile++;

						d = Math.abs(num1 - (n + missingFile));
						d_square += (d * d);
					}
				}

				rank_correlation = 1 - (6 * d_square / (n * n * n - n));

				SpearMan_rank_correlation += rank_correlation;
			}
		}
		System.out
				.println("\nSpearman Coefficient  between lucene-Default and TF_IDF data: "
						+ Math.abs(SpearMan_rank_correlation
								/ lucene_data.size()) + "\n");
	}

	public static void main(String[] args) {
		QueryParagraphs q = new QueryParagraphs();
		int topSearch = 100;
		String[] queryArr = { "power nap benefits",
				"whale vocalization production of sound",
				"pokemon puzzle league" };

		try {
			q.indexAllParagraphs();

			ArrayList<Data.Page> pagelist = q
					.getPageListFromPath(QueryParagraphs.Cbor_OUTLINE);

			File f = new File(OUTPUT_DIR + "/result-lucene.run");
			if (f.exists()) {
				FileWriter createNewFile = new FileWriter(f);
				createNewFile.write("");
			}
			for (Data.Page page : pagelist) {

				q.rankParas(page, 100, "result-lucene.run");
			}

			q.customScore(true);
			f = new File(OUTPUT_DIR + "/result-custom.run");
			if (f.exists()) {
				FileWriter createNewFile = new FileWriter(f);
				createNewFile.write("");
			}
			for (Data.Page page : pagelist) {

				q.rankParas(page, 100, "result-custom.run");
			}

			TFIDF_anc_apc tfidf_anc_apc = new TFIDF_anc_apc();
			tfidf_anc_apc.retrieveAllAncApcResults(pagelist, OUTPUT_DIR
					+ "/tfidf_anc_apc.run");

			TFIDF_bnn_bnn tfidf_bnn_bnn = new TFIDF_bnn_bnn(pagelist, 100);
			tfidf_bnn_bnn.doScoring();

			TFIDF_lnc_ltn tfidf_lnc_ltn = new TFIDF_lnc_ltn(pagelist, 100);
			tfidf_lnc_ltn.dumpScoresTo(OUTPUT_DIR + "/tfidf_lnc_ltn.run");

			// SpearMan Coefficient Implementation

			String lucenedefault = "output//result-lucene.run";
			HashMap<String, HashMap<String, String>> lucene_data = read_dataFile(lucenedefault);

			String tfIdf_anc_apc = "output/tfidf_anc_apc.run";
			HashMap<String, HashMap<String, String>> tfIdf_anc_apcData = read_dataFile(tfIdf_anc_apc);

			String tfidf_lnc_ltn1 = "output/tfidf_lnc_ltn.run";
			HashMap<String, HashMap<String, String>> lnc_ltnData = read_dataFile(tfidf_lnc_ltn1);

			String tfidf_bnn_bnn1 = "output/tfidf_bnnbnn.run";
			HashMap<String, HashMap<String, String>> bnn_bnnData = read_dataFile(tfidf_bnn_bnn1);

			System.out
					.println("Correlation between lucene-default and anc_apc");
			calculateCorrelation(lucene_data, tfIdf_anc_apcData);

			System.out
					.println("Correlation between lucene-default and lnc_ltn");
			calculateCorrelation(lucene_data, lnc_ltnData);

			System.out
					.println("Correlation between lucene-default and bnn_bnn");
			calculateCorrelation(lucene_data, bnn_bnnData);

		} catch (CborException | IOException | ParseException e) {
			e.printStackTrace();
		}

	}
}
