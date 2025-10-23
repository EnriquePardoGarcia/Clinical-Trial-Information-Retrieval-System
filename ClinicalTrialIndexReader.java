package es.udc.fi.irudc.c2425.ClinicalTrials; // Define el paquete donde se encuentra la clase.

import org.apache.lucene.document.Document; // Importa la clase Document de Lucene para representar documentos en el índice.
import org.apache.lucene.index.DirectoryReader; // Permite leer el índice almacenado en disco.
import org.apache.lucene.index.IndexReader; // Clase base para leer documentos en el índice.
import org.apache.lucene.index.StoredFields; // Permite acceder a los campos almacenados en el índice.
import org.apache.lucene.index.Term; // Representa un término en el índice de Lucene.
import org.apache.lucene.store.FSDirectory; // Permite acceder a un índice almacenado en el sistema de archivos.

import java.io.IOException; // Manejo de excepciones en operaciones de entrada/salida.
import java.nio.file.Path; // Permite manejar rutas de archivos.

/**
 * Clase que lee e itera sobre todos los elementos en el índice de Lucene.
 * Imprime el campo "Brief_Title" de cada documento y muestra estadísticas del índice.
 */
public class ClinicalTrialIndexReader {

    public static void main(String[] args) {
        // Ruta del directorio donde está almacenado el índice.
        String indexPath = "src/main/resources/index";

        long startTime = System.currentTimeMillis();

        try {
            // Abre el directorio donde se encuentra el índice de Lucene.
            FSDirectory dir = FSDirectory.open(Path.of(indexPath));

            // Abre el índice de Lucene en modo solo lectura.
            IndexReader reader = DirectoryReader.open(dir);

            System.out.println("Leyendo documentos del índice...\n");

            // Imprime estadísticas básicas del índice.
            printIndexStats(reader);

            // Itera sobre todos los documentos y muestra el título breve de cada uno.
            iterateAndPrintDocuments(reader);

            // Muestra la frecuencia de un término específico en un campo del índice.
            printDocumentFrequency(reader, "Brief_Title", "cancer");

            // Cierra el lector del índice para liberar recursos.
            reader.close();
            dir.close();

        } catch (IOException e) {
            e.printStackTrace(); // Maneja excepciones en caso de error al abrir el índice.
        }
        
        long endTime = System.currentTimeMillis();
        long elapsedMillis = endTime - startTime; // Calcula el tiempo total en milisegundos.
        double elapsedSeconds = elapsedMillis / 1000.0; // Convierte a segundos.

        System.out.println("Total time taken for indexing: " + elapsedSeconds + " seconds");
    }

    /**
     * Imprime estadísticas básicas del índice de Lucene.
     *
     * @param reader Objeto IndexReader para acceder al índice.
     */
    private static void printIndexStats(IndexReader reader) {
        System.out.println("===== Estadísticas del Índice =====");
        // Imprime el número total de documentos indexados.
        System.out.println("Número total de documentos en el índice: " + reader.numDocs());
        // Imprime la cantidad de documentos eliminados que aún no han sido purgados del índice.
        System.out.println("Número de documentos eliminados: " + reader.numDeletedDocs());
        System.out.println("===================================\n");
    }

    /**
     * Itera sobre todos los documentos en el índice e imprime el campo "Brief_Title".
     *
     * @param reader Objeto IndexReader para acceder al índice.
     */
    private static void iterateAndPrintDocuments(IndexReader reader) throws IOException {
        System.out.println("===== Documentos en el Índice =====");

        // Accede a los campos almacenados en cada documento.
        StoredFields storedFields = reader.storedFields();

        // Itera sobre todos los documentos en el índice.
        for (int i = 0; i < reader.maxDoc(); i++) {
            // Obtiene los campos almacenados del documento con el ID actual.
            Document doc = storedFields.document(i);

            // Extrae el valor del campo "Brief_Title".
            String briefTitle = doc.get("Brief_Title");
            if (briefTitle != null) {
                // Imprime el ID del documento y su título breve.
                System.out.println("Document ID: " + i + " | Brief Title: " + briefTitle);
            }
        }

        System.out.println("===================================\n");
    }

    /**
     * Muestra la frecuencia de un término en un campo específico del índice.
     *
     * @param reader Objeto IndexReader para acceder al índice.
     * @param field  Nombre del campo donde se buscará el término.
     * @param term   Término que se quiere analizar en el índice.
     */
    private static void printDocumentFrequency(IndexReader reader, String field, String term) throws IOException {
        System.out.println("===== Frecuencia del Término =====");

        // Crea un objeto Term para buscar en el índice, asegurando que el término esté en minúsculas.
        Term searchTerm = new Term(field, term.toLowerCase());
        // Obtiene la cantidad de documentos en los que aparece el término.
        int docFreq = reader.docFreq(searchTerm);

        // Imprime cuántos documentos contienen el término en el campo especificado.
        System.out.println("El término: \"" + term + "\" aparece en " + docFreq + " documentos en el campo \"" + field + "\".");
        System.out.println("===================================\n");
    }
}
