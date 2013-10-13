package org.wso2.carbon.appfactory.utilities.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class AppVersionStrategyExecutor {
	
	public void doVersion(String targetVersion, File workDir) {
		MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
		Model model;
		try {
			String[] fileExtension = { "xml" };
			List<File> fileList = (List<File>) FileUtils.listFiles(workDir,
					fileExtension, true);

			for (File file : fileList) {

				if (file.getName().equals("pom.xml")) {
					FileInputStream stream = new FileInputStream(file);
					model = mavenXpp3Reader.read(stream);
					model.setVersion(targetVersion);
					if (stream != null) {
						stream.close();
					}
					MavenXpp3Writer writer = new MavenXpp3Writer();
					writer.write(new FileWriter(file), model);
				}
			}
		} catch (Exception e) {
			//TODO 
			e.printStackTrace();
		}
	}

}
