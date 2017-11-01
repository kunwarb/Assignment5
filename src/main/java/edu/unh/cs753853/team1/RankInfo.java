package edu.unh.cs753853.team1;

public class RankInfo {
	private String queryStr;
	private String paraId;
	private String team_method_name;
	private float score;
	private int rank;
	private int docId;
	private String paraContent;

	public RankInfo() {

	}

	public RankInfo(String queryStr, String paraId, int rank, float score, String teamMethod, int docId,
			String paraContent) {
		this.queryStr = queryStr;
		this.paraId = paraId;
		this.docId = docId;
		this.team_method_name = teamMethod;
		this.rank = rank;
		this.score = score;
		this.paraContent = paraContent;
	}

	public String getQueryStr() {
		return queryStr;
	}

	public void setQueryStr(String queryStr) {
		this.queryStr = queryStr;
	}

	public String getParaId() {
		return paraId;
	}

	public void setParaId(String paraId) {
		this.paraId = paraId;
	}

	public String getTeam_method_name() {
		return team_method_name;
	}

	public void setTeam_method_name(String team_method_name) {
		this.team_method_name = team_method_name;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	public String getParaContent() {
		return paraContent;
	}

	public void setParaContent(String paraContent) {
		this.paraContent = paraContent;
	}

	@Override
	public String toString() {
		return queryStr + "  Q0 " + paraId + " " + rank + " " + score + " " + team_method_name;
	}
}
