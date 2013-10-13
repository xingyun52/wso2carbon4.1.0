package org.wso2.carbon.registry.indexing.indexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.wso2.carbon.registry.indexing.AsyncIndexer.File2Index;
import org.wso2.carbon.registry.indexing.solr.IndexDocument;

public class MSWordIndexer implements Indexer {

	public static final Log log = LogFactory.getLog(MSWordIndexer.class);

	public IndexDocument getIndexedDocument(File2Index fileData)
			throws SolrException {
		try {
			POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(fileData.data));
			WordExtractor extractor = new WordExtractor(fs);
			String wordText = extractor.getText();

			return new IndexDocument(fileData.path, wordText, null);
		} catch (IOException e) {
			String msg = "Failed to write to the index";
			log.error(msg, e);
			throw new SolrException(ErrorCode.SERVER_ERROR, msg);
		}
	}

}
