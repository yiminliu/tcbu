package com.tc.bu.dao;

public class GeneralSPResponse {

	private int mvnemsgcode;
	
	private String status;
	private String mvnemsg;
	
	public GeneralSPResponse() {}

	public int getMvnemsgcode() {
		return mvnemsgcode;
	}

	public void setMvnemsgcode(int mvnemsgcode) {
		this.mvnemsgcode = mvnemsgcode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMvnemsg() {
		return mvnemsg;
	}

	public void setMvnemsg(String mvnemsg) {
		this.mvnemsg = mvnemsg;
	}
	
	
	
}

