// Declaración del paquete donde se encuentra la clase
package es.udc.fi.irudc.c2425.ClinicalTrials;

// Importaciones necesarias para manejo de archivos, rutas, colecciones, etc.
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClinicalTrialEmbeddingSearcher {

    // Mapa que almacena los embeddings de todas las queries (tópicos) usando su ID como clave
    private static Map<String, float[]> embeddingsMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Ruta donde se encuentra el índice generado previamente con Lucene
        String indexPath = "src/main/resources/index";

        // Ruta del fichero XML que contiene los tópicos (casos clínicos)
        String topicsPath = "src/main/resources/topics_queries_and_narratives.xml";

        // Ruta del archivo JSON con los embeddings de las queries
        String embeddingsFile = "src/main/resources/query_embeddings.json";

        // Ruta del archivo donde se guardarán los resultados de la búsqueda
        String outputPath = "src/main/resources/vector_embeddings_metodo1.txt";

        // Abrimos el índice con un IndexReader y lo usamos para inicializar un IndexSearcher
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);                         

        // Cargamos todos los embeddings de las queries a memoria desde el JSON
        loadAllQueryEmbeddings(embeddingsFile);

        // Parseamos los tópicos desde el XML usando una clase auxiliar
        List<Topic2> topics = TopicParser2.parseTopics(topicsPath);

        // Preparamos el BufferedWriter para escribir los resultados de búsqueda
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

        // Iteramos sobre todos los tópicos (casos clínicos)
        for (Topic2 topic : topics) {
            // Obtenemos el número de tópico
            int topicNumber = topic.getNumber();

            // Obtenemos el vector embedding correspondiente a dicho tópico
            float[] queryEmbedding = getEmbeddingForTopic(topicNumber);

            // Si no existe un embedding para este tópico, se ignora
            if (queryEmbedding == null) {
                System.out.println("Saltando tópico " + topicNumber + ": no se encontró embedding.");
                continue;
            }

            // Mensaje de control para seguimiento de ejecución
            System.out.println("Tópico " + topicNumber + " | Ejecutando búsqueda vectorial...");

            // Creamos una consulta vectorial (KNN) sobre el campo "brief_title_vector"
            KnnVectorQuery vectorQuery = new KnnVectorQuery("brief_title_vector", queryEmbedding, 100);

            // Ejecutamos la búsqueda y recuperamos los 100 documentos más similares
            TopDocs topDocs = searcher.search(vectorQuery, 100);

            // Escribimos los resultados en el archivo de salida en formato TREC
            writeResultsTREC(writer, searcher, topDocs, topicNumber);
        }

        // Cerramos el escritor y el lector del índice
        writer.close();
        reader.close();

        // Mensaje de finalización
        System.out.println("Búsqueda vectorial completada.");
    }

    /**
     * Método que carga todos los embeddings desde un único archivo JSON.
     * El archivo debe tener formato: { "1": [float, float, ...], "2": [...], ... }
     */
    private static void loadAllQueryEmbeddings(String filePath) {
        try {
            // Usamos Jackson para parsear el JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new FileReader(filePath)); // Para leer y transformar de formato Json a formato JAVA

            // Iteramos sobre todas las claves (número de tópico)
            // Se convierte en un árbol de nodos donde cada nodo es una consulta/tópico con su embedding
            for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
                String topicId = it.next(); // ID del tópico como cadena
                JsonNode embeddingArray = root.get(topicId); // Array de floats del embedding

                // Convertimos el array JSON a un array Java de floats
                float[] vector = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    vector[i] = (float) embeddingArray.get(i).asDouble();
                }

                // Guardamos el vector en el mapa usando el ID como clave
                embeddingsMap.put(topicId, vector);
            }
        } catch (IOException e) {
            // Mostramos error en consola si ocurre un problema leyendo el archivo
            System.err.println("Error cargando el archivo de embeddings: " + e.getMessage());
        }
    }

    /**
     * Recupera el embedding asociado a un tópico específico a partir del mapa en memoria.
     * @param topicNumber Número del tópico (entero)
     * @return Vector de floats que representa el embedding del tópico
     */
    private static float[] getEmbeddingForTopic(int topicNumber) {
        // Convertimos el número a String para acceder al mapa
        return embeddingsMap.get(String.valueOf(topicNumber));
    }

    /**
     * Escribe los resultados de búsqueda en formato TREC.
     * Cada línea sigue el formato: <topic_id> Q0 <nct_id> <rank> <score> <run_name>
     */
    private static void writeResultsTREC(BufferedWriter writer, IndexSearcher searcher,
                                        TopDocs topDocs, int topicNumber) throws IOException {
        int rank = 1;

        // Iteramos sobre los documentos devueltos
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            // Obtenemos el documento Lucene a partir del ID
            Document doc = searcher.doc(scoreDoc.doc);

            // Recuperamos el identificador del ensayo clínico
            String nctId = doc.get("nct_id");

            // Obtenemos la puntuación asignada por la búsqueda vectorial
            float score = scoreDoc.score;

            // Escribimos el resultado en el archivo
            writer.write(String.format("%d Q0 %s %d %.4f metodo1\n", topicNumber, nctId, rank, score));
            rank++;
        }
    }
}
