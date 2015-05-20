/**
 * ****************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 * *****************************************************************************
 */
package com.codenvy.test.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * //
 *
 * @author Andrienko Alexander
 */
public class DeleteFilesWithSameName {

    private static final String PATH         = "path";
    private static String filesDirPath;

    public static void main(String[] args) throws Exception {
        String DOC_DIR_NAME = "files";
        filesDirPath = Paths.get(DOC_DIR_NAME).toAbsolutePath().toString();

        Path indexPath = Paths.get("index");
        Path docDir = Paths.get(DOC_DIR_NAME);

        Path file1 = Paths.get(DOC_DIR_NAME, "File1");
        Path file2 = Paths.get(DOC_DIR_NAME, "File1A");

        Analyzer analyzer = new SimpleAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        //iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        if (!Files.isReadable(docDir)) {
            System.out.println("document folder not found");
            return;
        }

        Directory index = FSDirectory.open(indexPath);

        IndexWriter writer = new IndexWriter(index, iwc);

        //add files to index
        indexDocs(writer, file1);
        indexDocs(writer, file2);
        writer.commit();

        searchAndPrintResult(indexPath);

        //delete files
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("delete by prefix \""+ filesDirPath + "/File1\"");
        Query query = new PrefixQuery(new Term(PATH, filesDirPath + "/File1"));

        writer.deleteDocuments(query);
        writer.close();

        searchAndPrintResult(indexPath);
    }

    public static void searchAndPrintResult(Path indexPath) throws Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new PrefixQuery(new Term(PATH, filesDirPath + "/File1"));
        TopDocs topDocs = searcher.search(query, 100);

        System.out.println();
        System.out.println("=========================== search =================================");
        final String[] result = new String[topDocs.scoreDocs.length];
        for (int i = 0, length = result.length; i < length; i++) {
            result[i] = searcher.doc(topDocs.scoreDocs[i].doc).getField(PATH).stringValue();
            System.out.println(result[i]);
        }

        reader.close();
    }

    private static void indexDocs(IndexWriter writer, Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            Document doc = new Document();

            System.out.println("file path " + file.toAbsolutePath().toString());
            Field pathField = new StringField(PATH, file.toAbsolutePath().toString(), Field.Store.YES);
            doc.add(pathField);

            doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {

                System.out.println("adding " + file);
                writer.addDocument(doc);
            } else {
                System.out.println("updating " + file);
                writer.updateDocument(new Term(PATH, file.toString()), doc);
            }
        }
    }
}
