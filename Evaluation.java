package es.udc.fi.irudc.c2425.ClinicalTrials; // Paquete de la clase

// Importaciones necesarias para manejo de archivos, colecciones y excepciones
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluation {

    // Método que lee el archivo qrels.txt y construye un mapa con los documentos relevantes por consulta
    public static Map<String, Map<String, Integer>> parseQrels(String qrelsFile) throws IOException {
        Map<String, Map<String, Integer>> qrels = new HashMap<>(); // Mapa: queryID -> mapa de docIDs y su relevancia
        BufferedReader br = new BufferedReader(new FileReader(qrelsFile)); // Lector del archivo qrels línea a línea
        String line;
        while ((line = br.readLine()) != null) { // Recorre cada línea
            String[] parts = line.split("\\s+"); // Separa la línea por espacios
            if (parts.length < 4) continue; // Si no hay suficientes campos, se ignora
            String queryId = parts[0]; // Primer campo: ID de la consulta
            String docId = parts[2];   // Tercer campo: ID del documento
            int relevance = Integer.parseInt(parts[3]); // Cuarto campo: grado de relevancia
            if (relevance > 0) { // Solo se consideran documentos relevantes (relevancia > 0)
                qrels.computeIfAbsent(queryId, k -> new HashMap<>()).put(docId, relevance); // Añade el doc con su score
            }
        }
        br.close(); // Cierra el lector
        return qrels; // Devuelve el mapa completo
    }

    // Método que lee el archivo de resultados y los organiza por consulta
    public static Map<String, List<String>> parseRanking(String rankingFile) throws IOException {
        Map<String, List<String>> rankings = new HashMap<>(); // Mapa: queryID -> lista ordenada de docIDs recuperados
        BufferedReader br = new BufferedReader(new FileReader(rankingFile)); // Lector del archivo
        String line;
        while ((line = br.readLine()) != null) { // Lee cada línea del archivo de resultados
            String[] parts = line.split("\\s+"); // Separa por espacios
            if (parts.length < 6) continue; // Verifica que la línea tenga el formato TREC correcto
            String queryId = parts[0]; // Primer campo: ID de la consulta
            String docId = parts[2];   // Tercer campo: ID del documento recuperado
            rankings.computeIfAbsent(queryId, k -> new ArrayList<>()).add(docId); // Añade el documento a la lista de esa consulta
        }
        br.close(); // Cierra el lector
        return rankings; // Devuelve el mapa de rankings
    }

    // MÉTRICA: Precision@k mide la proporción de documentos relevantes entre los k primeros recuperados
    // De los k primeros documentos, cuantos son relevantes
    // CALIDAD DE LOS RESULTADOS
    public static double precisionAtK(List<String> retrieved, Map<String, Integer> relevant, int k) {
        int relevantCount = 0;
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) { // Hasta k o el tamaño real de la lista
            if (relevant.getOrDefault(retrieved.get(i), 0) > 0) { // Si el documento está entre los relevantes
                relevantCount++; // Se cuenta como acierto
            }
        }
        return (double) relevantCount / k; // Devuelve la precisión como fracción
    }

    // MÉTRICA: Recall@k mide cuántos de los documentos relevantes fueron recuperados entre los k primeros
    // Proporción de documentos relevantes respecto al total de docs relevantes, en los k primeros documentos
    // CANTIDAD DE LOS RELEVANTES ENCONTRADOS
    public static double recallAtK(List<String> retrieved, Map<String, Integer> relevant, int k) {
        int relevantCount = 0;
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) {
            if (relevant.getOrDefault(retrieved.get(i), 0) > 0) {
                relevantCount++;
            }
        }
        if (relevant.isEmpty()) return 0; // Si no hay documentos relevantes, se devuelve 0
        return (double) relevantCount / relevant.size(); // Fracción de los relevantes recuperados
    }

    // MÉTRICA: Average Precision mide la media de la precisión cada vez que se recupera un documento relevante
    // Se calcula la precisión cada vez que aparece un doc relevante, y al final del todo se hace la media
    public static double mapAtK(List<String> retrieved, Map<String, Integer> relevant, int k) {
        double sum = 0.0;
        int hits = 0;
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) {
            if (relevant.getOrDefault(retrieved.get(i), 0) > 0) { // Cada vez que aparece un relevante
                hits++;
                sum += hits / (double) (i + 1); // Se acumula la precisión en esa posición
            }
        }
        return (relevant.size() == 0) ? 0.0 : sum / relevant.size(); // Promedia la precisión
    }

    // MÉTRICA: DCG@k mide la ganancia acumulada de los documentos relevantes, penalizando por la posición con un descuento logarítmico
    // Valora la posición de los docs relevantes apareciendo en los primeros puestos, y penalizando los que están más abajo
    // No castiga docs irrelevantes si no que penaliza a los relevantes que están abajo
    public static double dcgAtK(List<String> retrieved, Map<String, Integer> relevant, int k) {
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) {
            int rel = relevant.getOrDefault(retrieved.get(i), 0); // Se puede usar relevancia con peso
            if (i == 0) {
                dcg += rel; // Primer documento no tiene penalización
            } else {
                dcg += rel / (Math.log(i + 1) / Math.log(2)); // Penalización logarítmica por la posición
            }
        }
        return dcg; // Devuelve la ganancia acumulada
    }

    // MÉTRICA: NDCG@k mide la calidad de la ordenación respecto a la mejor ordenación posible (normaliza DCG)
    // Se divide entre el DCG ideal, es decir, el de los qrels ordenados por relevancia
    public static double ndcgAtK(List<String> retrieved, Map<String, Integer> relevant, int k) {
        double dcg = dcgAtK(retrieved, relevant, k); // Calcula el DCG real

        // Creamos lista de relevancias ideales ordenadas de mayor a menor
        List<Integer> idealRelevances = new ArrayList<>(relevant.values());
        idealRelevances.sort((a, b) -> Integer.compare(b, a)); // Orden descendente

        double idealDcg = 0.0;
        for (int i = 0; i < Math.min(k, idealRelevances.size()); i++) {
            int rel = idealRelevances.get(i);
            if (i == 0) {
                idealDcg += rel;
            } else {
                idealDcg += rel / (Math.log(i + 1) / Math.log(2));
            }
        }
        return idealDcg == 0 ? 0 : dcg / idealDcg; // DCG normalizado: cuánto se acerca al ideal
    }

    // Método principal que ejecuta la evaluación completa
    public static void main(String[] args) {
        String qrelsFile = "src/main/resources/qrels.txt";       // Ruta del archivo de juicios de relevancia
        String rankingFile = "src/main/resources/results_hybrid.txt";  // Ruta del archivo de resultados generados

        try {
            Map<String, Map<String, Integer>> qrels = parseQrels(qrelsFile);         // Mapa de consulta → docs relevantes con score
            Map<String, List<String>> rankings = parseRanking(rankingFile); // Mapa de consulta → docs recuperados

            // Variables para acumular métricas globales
            double sumPAtk = 0.0;
            double sumRecallAtk = 0.0;
            double sumAP = 0.0;
            double sumNDCGAtk = 0.0;
            int queryCount = 0;

            // Itera por cada consulta procesada
            for (String queryId : rankings.keySet()) {
                List<String> retrieved = rankings.get(queryId); // Documentos recuperados para esa consulta
                Map<String, Integer> relevant = qrels.getOrDefault(queryId, new HashMap<>()); // Mapa de relevantes

                int k = Math.min(10, retrieved.size()); // Evaluar hasta los primeros 10 documentos

                // Calcula las métricas para esta consulta
                double pAtk = precisionAtK(retrieved, relevant, k);    
                double recallAtk = recallAtK(retrieved, relevant, k);  
                double ap = mapAtK(retrieved, relevant, k);       
                double ndcgAtk = ndcgAtK(retrieved, relevant, k);      

                // Imprime resultados individuales
                System.out.println("Query " + queryId + ":");
                System.out.printf("  P@%d: %.4f\n", k, pAtk);
                System.out.printf("  Recall@%d: %.4f\n", k, recallAtk);
                System.out.printf("  AP@%d: %.4f\n", k, ap);
                System.out.printf("  NDCG@%d: %.4f\n", k, ndcgAtk);
                System.out.println();

                // Acumula para promediar
                sumPAtk += pAtk;
                sumRecallAtk += recallAtk;
                sumAP += ap;
                sumNDCGAtk += ndcgAtk;
                queryCount++; // Cuenta la consulta procesada
            }

            // Imprime métricas globales promediadas
            System.out.println("Métricas promedio:");
            System.out.printf("  Mean P@10: %.4f\n", sumPAtk / queryCount);
            System.out.printf("  Mean Recall@10: %.4f\n", sumRecallAtk / queryCount);
            System.out.printf("  MAP@10: %.4f\n", sumAP / queryCount);
            System.out.printf("  Mean NDCG@10: %.4f\n", sumNDCGAtk / queryCount);

        } catch (IOException e) {
            e.printStackTrace(); // Muestra el error si falla la lectura de archivos
        }
    }
}
