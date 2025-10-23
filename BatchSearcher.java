package es.udc.fi.irudc.c2425.ClinicalTrials; // Paquete de la clase

// Importaciones de Lucene para búsqueda y análisis
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

// Importaciones estándar de Java para archivos y colecciones
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

public class BatchSearcher {

    public static void main(String[] args) throws Exception {

        // Ruta del índice Lucene
        String indexPath = "src/main/resources/index";

        // Ruta del archivo de tópicos XML
        String topicsPath = "C:\\Users\\enriq\\OneDrive\\Escritorio\\CIENCIA E INGENIERÍA DE DATOS\\TERCERO\\2º CUATRI\\RI\\trec-clinical-trials-ir-practica-pardo-fernandez\\src\\main\\resources\\topics_queries_and_narratives.xml";

        // Ruta del archivo de salida .txt
        String outputRunFile = "src/main/resources/results1.txt";

        // Apertura del índice desde el sistema de archivos
        FSDirectory dir = FSDirectory.open(Path.of(indexPath)); // Para leer el indice de los trials
        IndexReader reader = DirectoryReader.open(dir); // Lector del índice
        IndexSearcher searcher = new IndexSearcher(reader); // Buscador del índice
        StandardAnalyzer analyzer = new StandardAnalyzer(); // Analizador para procesar consultas

        // Carga de los tópicos desde XML
        List<Topic> topics = TopicParser.parseTopics(topicsPath); // Se crea una isntancia a partir del TopicParser para posteriormente parsear los topics del XML

        // Preparación del archivo de salida
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputRunFile));

        // Envuelve al FileWriter para mejorar el rendimiento.
        // En lugar de escribir directamente en el disco con cada línea, guarda los datos en un buffer en memoria y los escribe por bloques.
        // Esto reduce el acceso al disco y hace la escritura mucho más eficiente.
        
        // Iteración por cada tópico
        for (Topic topic : topics) {
            String queryText = topic.getQuery(); // Obtención de la consulta textual

            try {
                // Campos sobre los que se realizará la búsqueda (coinciden con los del indexador)
                String[] fields = {"brief_title", "detailed_description", "criteria"};

                // Parser para múltiples campos
                QueryParser parser = new MultiFieldQueryParser(fields, analyzer);
                Query query = parser.parse(QueryParser.escape(queryText));

                // Por ejemplo algo entre () se puede tomar en el parseo como un agrupamiento, dando errores de parsing

                // Ejecución de la búsqueda y obtención de los 100 resultados más relevantes
                TopDocs topDocs = searcher.search(query, 100);
                ScoreDoc[] hits = topDocs.scoreDocs;

                // El método devuelve un objeto de tipo TopDocs, que contiene:
                    // Un array con los documentos recuperados (scoreDocs).
                    // Información sobre el número total de documentos que coincidieron.

                // Cada ScoreDoc representa un documento individual recuperado, junto con su ID interno y su puntuación de relevancia

                // Iteración sobre los resultados obtenidos
                for (int rank = 0; rank < hits.length; rank++) {
                    Document doc = searcher.doc(hits[rank].doc); // Obtención del documento

                    String docId = doc.get("NCT_ID"); // ID del ensayo clínico
                    float score = hits[rank].score; // Puntuación de relevancia

                    // Escritura del resultado en formato TREC
                    writer.write(String.format(java.util.Locale.US, "%d Q0 %s %d %.4f mi_metodo\n",
                        topic.getNumber(), docId, rank + 1, score));
                }

            } catch (Exception e) {
                // En caso de error, se notifica qué tópico falló
                System.err.println("Error parsing topic " + topic.getNumber());
                e.printStackTrace();
            }
        }

        // Cierre de recursos
        writer.close();
        reader.close();
        dir.close();

        // Confirmación por consola
        System.out.println(".run file created at: " + outputRunFile);
    }
}
