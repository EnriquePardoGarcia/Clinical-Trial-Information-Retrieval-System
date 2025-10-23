// Paquete donde se encuentra la clase, siguiendo la convención del proyecto
package es.udc.fi.irudc.c2425.ClinicalTrials;

// Importaciones para gestión de archivos y estructuras de datos
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClinicalTrialRescoreSearcher {

    // Mapa que almacenará todos los embeddings de las queries (topics) en memoria
    private static Map<String, float[]> embeddingsMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Rutas de entrada/salida
        String indexPath = "src/main/resources/index"; // Índice Lucene
        String topicsPath = "src/main/resources/topics_queries_and_narratives.xml"; // Casos clínicos
        String embeddingsPath = "src/main/resources/query_embeddings.json"; // Embeddings de las queries
        String outputPath = "src/main/resources/results3_rescore.txt"; // Archivo de resultados

        // Cargamos todos los embeddings en memoria desde el archivo JSON
        loadAllQueryEmbeddings(embeddingsPath);

        // Abrimos el índice Lucene
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        // Analizador para las búsquedas textuales
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // Cargamos los tópicos (queries clínicas)
        List<Topic2> topics = TopicParser2.parseTopics(topicsPath);

        // Preparamos el archivo de salida para escribir los resultados
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

        // Procesamos cada tópico (caso clínico individual)
        for (Topic2 topic : topics) {
            int topicNumber = topic.getNumber();         // Número del tópico
            int age = topic.getAge();                    // Edad del paciente
            String gender = topic.getGender().toLowerCase(); // Género del paciente
            String queryText = topic.getQuery();         // Texto para la consulta textual
            float[] embedding = embeddingsMap.get(String.valueOf(topicNumber)); // Vector embedding

            // Verificamos que tengamos toda la información necesaria para realizar la búsqueda
            if (age == -1 || queryText == null || queryText.isEmpty() || embedding == null) {
                System.out.println("Saltando tópico " + topicNumber + " por datos insuficientes.");
                continue;
            }

            System.out.println("\nTópico " + topicNumber + " | Ejecutando búsqueda textual...");

            // Ejecutamos la búsqueda textual con filtros
            TopDocs initialResults = searchInitialQuery(searcher, analyzer, queryText, age, gender, 10000);

            // Lista para guardar documentos reordenados por similitud semántica
            List<ScoredDocument> rescored = new ArrayList<>(); // Esto devuelve un objeto de tipo TopDocs, que contiene ScoreDocs,
            // array que empareja el doc del clinical trial con su score

            // Recorremos los documentos recuperados inicialmente
            for (ScoreDoc sd : initialResults.scoreDocs) { // Se recorren los score docs con el nombre sd
                Document doc = searcher.doc(sd.doc); // Con esto se accede a los docs al completo
                String nctId = doc.get("nct_id"); // Se obtiene el id
                String vecStr = doc.get("brief_title_vector_stored"); // vector embebido en el índice como string

                // Si el documento tiene vector, calculamos la similitud
                if (nctId != null && vecStr != null) {
                    float[] docVec = parseStoredVector(vecStr);          // Convertimos el string a float[]
                    float score = dotProduct(embedding, docVec);        // Producto punto como medida de similitud
                    rescored.add(new ScoredDocument(sd.doc, score));    // Añadimos el documento con su nuevo score
                } else {
                    System.out.println("Documento " + (nctId != null ? nctId : "[sin ID]") + " sin vector almacenado.");
                }
            }

            // Ordenamos los documentos según la puntuación del rescoring (descendente)
            rescored.sort((a, b) -> Float.compare(b.score, a.score)); // Reordenación descendente
            // Da negativo si b < a, entonces a va antes que b

            // Escribimos los top 10 resultados reordenados en el archivo TREC
            writeResultsTREC(writer, searcher, rescored, topicNumber, 10);
        }

        // Cerramos recursos
        writer.close();
        reader.close();
        System.out.println("\nIteración 3 completada: rescoring aplicado y resultados escritos.");
    }

    /**
     * Realiza la búsqueda textual inicial aplicando filtros por edad y género.
     */
    private static TopDocs searchInitialQuery(IndexSearcher searcher, StandardAnalyzer analyzer,
                                            String queryText, int age, String gender, int topK) throws Exception {
        // Constructor de consulta booleana
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        // Campos a consultar en la búsqueda textual
        String[] fields = {"brief_title", "detailed_description", "criteria"};

        // Analizamos el texto de la query en múltiples campos
        Query parsedQuery = new MultiFieldQueryParser(fields, analyzer).parse(queryText);
        queryBuilder.add(parsedQuery, BooleanClause.Occur.MUST);

        // Filtro por edad mínima y máxima
        queryBuilder.add(IntPoint.newRangeQuery("minimum_age", Integer.MIN_VALUE, age), BooleanClause.Occur.FILTER);
        queryBuilder.add(IntPoint.newRangeQuery("maximum_age", age, Integer.MAX_VALUE), BooleanClause.Occur.FILTER);

        // Filtro por género: aceptamos coincidencia exacta o "all"
        if (!gender.equals("unknown") && !gender.equals("all")) {
            BooleanQuery.Builder genderQ = new BooleanQuery.Builder();
            genderQ.add(new TermQuery(new Term("gender", gender)), BooleanClause.Occur.SHOULD);
            genderQ.add(new TermQuery(new Term("gender", "all")), BooleanClause.Occur.SHOULD);
            queryBuilder.add(genderQ.build(), BooleanClause.Occur.SHOULD);
        }

        // Ejecutamos la búsqueda y devolvemos los topK resultados
        return searcher.search(queryBuilder.build(), topK);
    }

    /**
     * Convierte un string con vectores separados por comas a un array de floats.
     */
    private static float[] parseStoredVector(String vectorString) {
        String[] parts = vectorString.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i]);
        }
        return vec;
    }

    /**
     * Calcula el producto punto entre dos vectores.
     */
    private static float dotProduct(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length && i < b.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Escribe los resultados reordenados en formato TREC.
     */
    private static void writeResultsTREC(BufferedWriter writer, IndexSearcher searcher,
                                         List<ScoredDocument> rescoredDocs, int topicNumber, int topK) throws IOException {
        int rank = 1;
        for (ScoredDocument sd : rescoredDocs) {
            if (rank > topK) break; // Solo topK documentos
            Document doc = searcher.doc(sd.docId);
            String nctId = doc.get("nct_id");
            writer.write(String.format("%d Q0 %s %d %.4f metodo3_rescore\n", topicNumber, nctId, rank, sd.score));
            rank++;
        }
    }

    /**
     * Carga todos los embeddings de las queries desde el archivo JSON a memoria.
     */
    private static void loadAllQueryEmbeddings(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new FileReader(filePath));

            for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
                String topicId = it.next();
                JsonNode array = root.get(topicId);
                float[] vec = new float[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    vec[i] = (float) array.get(i).asDouble();
                }
                embeddingsMap.put(topicId, vec);
            }
        } catch (IOException e) {
            System.err.println("Error leyendo embeddings: " + e.getMessage());
        }
    }

    /**
     * Clase interna que representa un documento con puntuación personalizada.
     */
    private static class ScoredDocument {
        int docId;     // ID interno del documento en el índice
        float score;   // Nueva puntuación tras rescoring

        ScoredDocument(int docId, float score) {
            this.docId = docId;
            this.score = score;
        }
    }
}
