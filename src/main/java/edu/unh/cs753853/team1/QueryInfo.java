package edu.unh.cs753853.team1;

import java.util.ArrayList;

public class QueryInfo {
	private String query;
	private ArrayList<RankInfo> rankResults;

	public QueryInfo() {

	}

	public QueryInfo(String query, ArrayList<RankInfo> ranklist) {
		this.query = query;
		this.rankResults = ranklist;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public ArrayList<RankInfo> getRankResults() {
		return rankResults;
	}

	public void setRankResults(ArrayList<RankInfo> rankResults) {
		this.rankResults = rankResults;
	}

}
