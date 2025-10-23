package es.udc.fi.irudc.c2425.ClinicalTrials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClinicalTrialIndexer {

    private static IndexWriter writer;
    private static int count = 0;
    private static Map<String, float[]> briefTitleEmbeddings = new HashMap<>();

    private static void processDirectoryAndIndex(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processDirectoryAndIndex(file);
            } else if (file.getName().toLowerCase().endsWith(".xml")) {
                ClinicalTrial trial = ClinicalTrialParser.parseFromFile(file.getAbsolutePath());
                if (trial != null) {
                    indexClinicalTrial(trial);
                    count++;
                }
            }
        }
    }

    private static void indexClinicalTrial(ClinicalTrial trial) {
        Document doc = new Document();

        String nctId = trial.getNctId();
        if (nctId != null) {
            doc.add(new StringField("nct_id", nctId, Field.Store.YES));
        }

        if (trial.getBriefTitle() != null) {
            String briefTitle = trial.getBriefTitle().toLowerCase();
            doc.add(new TextField("brief_title", briefTitle, Field.Store.YES));

            float[] vector = briefTitleEmbeddings.get(nctId);
            if (vector != null) {
                doc.add(new KnnVectorField("brief_title_vector", vector));

                // Serializar vector como texto separado por comas
                StringBuilder sb = new StringBuilder();
                for (float val : vector) {
                    sb.append(val).append(",");
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1); // eliminar última coma
                    doc.add(new StoredField("brief_title_vector_stored", sb.toString()));
                }

                System.out.println("Indexado con vector: " + nctId);
            } else {
                System.out.println("Sin vector (no indexado): " + nctId);
            }
        }

        if (trial.getDetailedDescription() != null) {
            doc.add(new TextField("detailed_description", trial.getDetailedDescription().toLowerCase(), Field.Store.YES));
        }

        if (trial.getcriteria() != null) {
            doc.add(new TextField("criteria", trial.getcriteria().toLowerCase(), Field.Store.YES));
        }

        if (trial.getGender() != null) {
            doc.add(new StringField("gender", trial.getGender().toLowerCase(), Field.Store.YES));
        }

        if (trial.getMinAgeInt() != -1) {
            doc.add(new IntPoint("minimum_age", trial.getMinAgeInt()));
            doc.add(new StoredField("minimum_age", trial.getMinAgeInt()));
        }

        if (trial.getMaxAgeInt() != -1) {
            doc.add(new IntPoint("maximum_age", trial.getMaxAgeInt()));
            doc.add(new StoredField("maximum_age", trial.getMaxAgeInt()));
        }

        try {
            writer.addDocument(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadBriefTitleEmbeddings(String embeddingsPath) {
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedReader reader = new BufferedReader(new FileReader(embeddingsPath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    JsonNode node = mapper.readTree(line);

                    if (node.has("nct_id") && node.has("embedding")) {
                        String id = node.get("nct_id").asText();
                        JsonNode embeddingArray = node.get("embedding");

                        float[] vector = new float[embeddingArray.size()];
                        for (int i = 0; i < embeddingArray.size(); i++) {
                            vector[i] = (float) embeddingArray.get(i).asDouble();
                        }

                        briefTitleEmbeddings.put(id, vector);
                    } else {
                        System.err.println("Línea " + lineNumber + ": falta 'nct_id' o 'embedding'.");
                    }

                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineNumber + ": " + line);
                    e.printStackTrace();
                }
            }

            System.out.println("Total embeddings cargados: " + briefTitleEmbeddings.size());

        } catch (IOException e) {
            System.err.println("Error leyendo el archivo de embeddings.");
            e.printStackTrace();
        }
    }

    private static void createIndex(String datasetRoot, String indexPath) {
        File rootDir = new File(datasetRoot);
        if (!rootDir.exists()) {
            System.err.println("El directorio del dataset no existe: " + datasetRoot);
            return;
        }

        try {
            Directory dir = FSDirectory.open(Path.of(indexPath));
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter w = new IndexWriter(dir, config)) {
                writer = w;

                System.out.println("Cargando embeddings...");
                loadBriefTitleEmbeddings("src/main/resources/brieftitle_embeddings.jsonl");

                System.out.println("Procesando dataset...");
                processDirectoryAndIndex(rootDir);

                writer.commit();
            }

            System.out.println("Indexación completada.");
            System.out.println("Total clinical trials indexados: " + count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String datasetRoot = "C:\\Users\\enriq\\OneDrive\\Escritorio\\dataset";
        String indexPath = "src/main/resources/index";

        long startTime = System.currentTimeMillis();
        createIndex(datasetRoot, indexPath);
        long endTime = System.currentTimeMillis();

        double elapsedSeconds = (endTime - startTime) / 1000.0;
        System.out.println("⏱ Tiempo total de indexación: " + elapsedSeconds + " segundos");
    }
}
