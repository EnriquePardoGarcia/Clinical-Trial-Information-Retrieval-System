// Declaración del paquete donde se encuentra esta clase
package es.udc.fi.irudc.c2425.ClinicalTrials;

// Importaciones necesarias de Apache Lucene
import org.apache.lucene.analysis.standard.StandardAnalyzer; // Analizador estándar para tokenizar el texto
import org.apache.lucene.document.Document; // Representa documentos en el índice
import org.apache.lucene.document.IntPoint; // Campo para realizar búsquedas por rango con enteros
import org.apache.lucene.index.DirectoryReader; // Lector de índices en disco
import org.apache.lucene.index.IndexReader; // Interfaz para acceder a documentos indexados
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser; // Permite consultar varios campos
import org.apache.lucene.queryparser.classic.ParseException; // Excepción por errores de parsing
import org.apache.lucene.search.*; // Operaciones de búsqueda de Lucene
import org.apache.lucene.store.FSDirectory; // Abre el índice desde el sistema de archivos

// Importaciones estándar de Java
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

// Clase principal que permite realizar búsquedas interactivas
public class ClinicalTrialMultiSearcher {

    public static void main(String[] args) {
        // Ruta donde está almacenado el índice Lucene
        String indexPath = "src/main/resources/index";

        try {
            // Abre el índice desde el sistema de archivos
            FSDirectory dir = FSDirectory.open(Path.of(indexPath));

            // Crea un lector para acceder a los documentos
            IndexReader reader = DirectoryReader.open(dir);

            // Crea un buscador para ejecutar consultas sobre el índice
            IndexSearcher searcher = new IndexSearcher(reader); // Se encarga de interpretar los objetos Query, recorrer el índice y devolver los documentos más relevantes.

            // Prepara el lector de entrada para la consola
            Scanner scanner = new Scanner(System.in);

            // Solicita al usuario una consulta textual
            System.out.print("Enter search query: ");
            String userQuery = scanner.nextLine();

            // Solicita al usuario la edad del paciente
            System.out.print("Enter patient age: ");
            int age = Integer.parseInt(scanner.nextLine());

            // Solicita al usuario el género del paciente (male/female/all)
            System.out.print("Enter patient gender (Male/Female/All): ");
            String genderInput = scanner.nextLine().trim().toLowerCase();

            // Cierra el escáner una vez capturada toda la entrada
            scanner.close();

            // Campos sobre los que se aplicará la búsqueda textual
            String[] fields = {"Brief_Title", "Detailed_Description", "Eligibility_Criteria"};

            // Crea un parser para procesar la consulta del usuario sobre esos campos
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());

            // Parsear la consulta del usuario a un objeto Query
            // Lo convierte a objeto de Lucene que pueda entender
            // Le dice que busque las palabras en cualquiera de los 3 campos
            Query textQuery = parser.parse(userQuery);

            // Filtro por edad mínima: se permite si la edad del paciente es mayor o igual que Min_Age
            Query ageFilter = IntPoint.newRangeQuery("Min_Age", Integer.MIN_VALUE, age); 

            // campo, valor menor, valor mayor

            // Filtro por edad máxima: se permite si la edad del paciente es menor o igual que Max_Age
            Query maxAgeFilter = IntPoint.newRangeQuery("Max_Age", age, Integer.MAX_VALUE);

            // Si el usuario ha especificado género, se crea un filtro correspondiente
            Query genderFilter = null;
            if (!genderInput.equals("all")) {
                // Se construye un filtro por igualdad exacta en el campo Gender
                genderFilter = new TermQuery(new org.apache.lucene.index.Term("Gender", genderInput));
            }

            // Se construye la consulta final como una consulta booleana
            BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();

            // El texto debe coincidir en alguno de los campos (obligatorio)
            finalQuery.add(textQuery, BooleanClause.Occur.MUST);

            // Se añaden los filtros de edad
            finalQuery.add(ageFilter, BooleanClause.Occur.FILTER);
            finalQuery.add(maxAgeFilter, BooleanClause.Occur.FILTER);

            // Si hay un filtro de género, también se añade
            if (genderFilter != null) {
                finalQuery.add(genderFilter, BooleanClause.Occur.FILTER);
            }
            // Es decir, si el género elegido es distinto de all, se aplica el filtro, si no, se deja como está y no se aplica filtro

            // Ejecutar la búsqueda y obtener los 10 mejores resultados
            // TopDocs representa el resultado de una búsqueda en Lucene
            // Lo crea el IndexSearcher
            TopDocs topDocs = searcher.search(finalQuery.build(), 10);

            // Cuando creas topDocs con el searcher, se devuelve un array con los 10 documentos con mayor score y con su respectivo id
            // Es lo que después en el display results se recorre de manera que se obtienen enumerados y en orden

            // Top Docs contiene el total hits y el score docs, con cada doc con su id y score

            // Mostrar los resultados por consola
            displayResults(searcher, topDocs, userQuery);

            // Cerrar recursos
            reader.close();
            dir.close();

        } catch (IOException | ParseException e) {
            // Manejo de errores: problemas con lectura del índice o parsing de la consulta
            e.printStackTrace();
        }
    }

    // Método auxiliar para mostrar los resultados de búsqueda por consola
    private static void displayResults(IndexSearcher searcher, TopDocs topDocs, String query) throws IOException {
        System.out.println("\nSearch Results for query: '" + query + "'");
        System.out.println("Total hits: " + topDocs.totalHits.value);

        // Recorre cada documento recuperado
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            // Recupera el documento real usando su ID
            Document doc = searcher.doc(scoreDoc.doc);

            // Extrae algunos campos para mostrar
            String nctId = doc.get("NCT_ID");
            String briefTitle = doc.get("Brief_Title");

            // Muestra el resultado
            System.out.println("DocID: " + scoreDoc.doc +
                               " | NCT ID: " + nctId +
                               " | Title: " + briefTitle +
                               " | Score: " + scoreDoc.score);
        }
    }
}
