package de.uni_stuttgart.caas.base;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;

public class QueryLog {

	public final long startTime;
	private long endTime = 0;
	private String[] path;
	public final long ID;

	public QueryLog(long start, long id) {
		startTime = start;
		ID = id;
	}
	
	public void finishQuery(long time, String[] path) {
		endTime = time;
		this.path = path;
	}
	
	public long getTransitTime() {
		assert endTime != 0;
		return endTime - startTime;
	}
	
	public String[] getPath() {
		assert path != null;
		return path;
	}
	
	public void writeToFile(BufferedWriter writer) {
		try {
			writer.append(ID + "," + getTransitTime() + "," + path.length + ",");
			for (int i = 0; i < path.length; ++i) {
				writer.append(path[i].replace("\n", ""));
				if (i != path.length - 1) {
					writer.append(",");
				}
			}
			writer.newLine();
			//writer.flush(); // too slow to do this all the time (acg)
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}