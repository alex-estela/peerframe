package fr.estela.peerframe.device.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;

public class StreamGobbler extends Thread {

	private InputStream inputStream;
	private Logger logger;
	private String streamId;
	private List<String> resultLines;
	
	public StreamGobbler(InputStream inputStream, Logger logger, List<String> result) {
		this.inputStream = inputStream;
		this.logger = logger;
		this.resultLines = result;
	}
	
	public StreamGobbler(InputStream inputStream, Logger logger, String streamId) {
		this.inputStream = inputStream;
		this.logger = logger;
		this.streamId = streamId;
	}

	@Override
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = reader.readLine()) != null) {
				if (streamId != null) logger.debug(">" + streamId + "> "+ line);
				else resultLines.add(line);
			}
			reader.close();
			return;
		} 
		catch (IOException e) {
			logger.error("An error occurred", e);
		}			
	}

}
