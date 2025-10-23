package es.udc.fi.irudc.c2425.ClinicalTrials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Esta clase se encarga de parsear un archivo XML de un ensayo clínico y convertirlo en un objeto ClinicalTrial.
 * Utiliza el parser DOM de Java para leer los elementos XML y extraer su contenido.
 */
public class ClinicalTrialParser {

    // Reutilización de la fábrica de DocumentBuilder para reducir la sobrecarga de creación.
    private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    static {
        dbFactory.setIgnoringElementContentWhitespace(true);
    }

    /**
     * Parsea el archivo XML dado y crea un objeto ClinicalTrial con varios campos.
     *
     * @param filePath la ruta al archivo XML que contiene la información del ensayo clínico
     * @return un objeto ClinicalTrial poblado con los datos del XML, o null si ocurre algún error
     */
    public static ClinicalTrial parseFromFile(String filePath) {
        ClinicalTrial trial = new ClinicalTrial();
        try {
            File xmlFile = new File(filePath);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            trial.setNctId(getTextContent(root, "nct_id"));
            trial.setOrgStudyId(getTextContent(root, "org_study_id"));
            trial.setSecondaryId(getTextContent(root, "secondary_id"));
            trial.setBriefTitle(getTextContent(root, "brief_title"));
            trial.setOfficialTitle(getTextContent(root, "official_title"));
            trial.setLeadSponsor(getTextContent(root, "lead_sponsor"));
            trial.setBriefSummary(getTextContent(root, "brief_summary"));
            trial.setDetailedDescription(getTextContent(root, "detailed_description"));
            trial.setOverallStatus(getTextContent(root, "overall_status"));
            trial.setStartDate(getTextContent(root, "start_date"));
            trial.setCompletionDate(getTextContent(root, "completion_date"));
            trial.setStudyType(getTextContent(root, "study_type"));
            trial.setPhase(getTextContent(root, "phase"));
            trial.setPrimaryOutcome(getTextContent(root, "primary_outcome"));
            String criteriaText = getTextContent(root, "criteria");
            trial.setcriteria(extractInclusionCriteria(criteriaText));
            trial.setMinimumAge(getTextContent(root, "minimum_age"));
            trial.setMaximumAge(getTextContent(root, "maximum_age"));
            trial.setLocation(getTextContent(root, "location"));

            trial.setConditions(getTextContentList(root, "condition"));
            trial.setInterventions(getTextContentList(root, "intervention"));

            trial.setGender(getTextContent(root, "gender"));
            trial.setMinAgeInt(parseAgeToInt(trial.getMinimumAge()));
            trial.setMaxAgeInt(parseAgeToInt(trial.getMaximumAge()));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return trial;
    }

    /**
     * Método auxiliar para obtener el contenido de texto de un elemento hijo dado un nombre de etiqueta.
     *
     * @param parent el elemento padre
     * @param tagName el nombre de la etiqueta del elemento hijo
     * @return el contenido de texto recortado del elemento hijo, o "N/A" si no se encuentra
     */
    private static String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0 && list.item(0) != null) {
            return list.item(0).getTextContent().trim();
        }
        return "N/A";
    }

    /**
     * Método auxiliar para obtener una lista de contenidos de texto de los elementos con el nombre de etiqueta dado.
     *
     * @param parent el elemento padre
     * @param tagName el nombre de la etiqueta de los elementos
     * @return una lista de contenidos de texto de los elementos que coinciden
     */
    private static List<String> getTextContentList(Element parent, String tagName) {
        List<String> values = new ArrayList<>();
        NodeList nodeList = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            values.add(nodeList.item(i).getTextContent().trim());
        }
        return values;
    }

    /**
    * Extrae solo la parte de "Inclusion Criteria" de los criterios de elegibilidad.
    *
    * @param criteriaText el texto completo de los criterios de elegibilidad
    * @return solo la sección de "Inclusion Criteria", si está presente; de lo contrario, devuelve el texto original.
    */
    private static String extractInclusionCriteria(String criteriaText) {
        if (criteriaText == null || criteriaText.isEmpty()) {
            return "";
        }
        
        String lower = criteriaText.toLowerCase();
        String inclusion = "";
        
        if (lower.contains("inclusion criteria:") && lower.contains("exclusion criteria:")) {
            // Divide el texto en dos partes usando "exclusion criteria:" como separador
            String[] parts = lower.split("exclusion criteria:");
            // Elimina "inclusion criteria:" de la primera parte y recorta espacios
            inclusion = parts[0].replace("inclusion criteria:", "").trim();
        } else if (lower.contains("inclusion criteria:")) {
            // Si solo aparece "inclusion criteria:", elimina la etiqueta y devuelve el resto
            inclusion = lower.replace("inclusion criteria:", "").trim();
        } else {
            // Si no se encuentra, se devuelve el texto original (o se puede retornar vacío)
            inclusion = criteriaText.trim();
        }
        
        return inclusion;
    }
    
    
    

    /**
     * Convierte una representación de edad (ej. "18 years", "6 months") en un entero.
     *
     * @param ageStr la cadena que representa la edad
     * @return el valor numérico de la edad, o -1 si no se puede parsear
     */
    private static int parseAgeToInt(String ageStr) {
        if (ageStr == null || ageStr.equalsIgnoreCase("N/A") || ageStr.equalsIgnoreCase("None"))
            return -1;
        try {
            String[] parts = ageStr.split(" ");
            int number = Integer.parseInt(parts[0]);
            if (parts.length > 1) {
                switch (parts[1].toLowerCase()) {
                    case "year": case "years": return number;
                    case "month": case "months": return number / 12;
                    case "week": case "weeks": return number / 52;
                    case "day": case "days": return number / 365;
                }
            }
            return number;
        } catch (Exception e) {
            return -1;
        }
    }
    

    /**
     * Recopila recursivamente todos los archivos XML dentro del directorio dado.
     *
     * @param folder el directorio raíz
     * @return una lista de archivos XML
     */
    private static List<File> collectXmlFiles(File folder) {
        List<File> xmlFiles = new ArrayList<>();
        File[] filesAndDirs = folder.listFiles();
        if (filesAndDirs == null) {
            System.out.println("⚠️ No se pudo acceder a la carpeta: " + folder.getAbsolutePath());
            return xmlFiles;
        }
        for (File file : filesAndDirs) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                xmlFiles.add(file);
            } else if (file.isDirectory()) {
                xmlFiles.addAll(collectXmlFiles(file));
            }
        }
        return xmlFiles;
    }

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\enriq\\OneDrive\\Escritorio\\dataset";
        File folder = new File(directoryPath);

        if (!folder.exists()) {
            System.out.println("❌ Error: La carpeta '" + directoryPath + "' no existe.");
            return;
        }

        if (!folder.isDirectory()) {
            System.out.println("❌ Error: '" + directoryPath + "' no es un directorio.");
            return;
        }

        // Recopilar todos los archivos XML en la carpeta y subcarpetas
        List<File> xmlFiles = collectXmlFiles(folder);

        // Procesar los archivos en paralelo para mayor velocidad
        xmlFiles.parallelStream().forEach(file -> {
            System.out.println("🔹 Procesando archivo: " + file.getAbsolutePath());
            ClinicalTrial trial = ClinicalTrialParser.parseFromFile(file.getAbsolutePath());
            if (trial != null) {
                trial.printValues();
            } else {
                System.out.println("❌ No se encontraron datos en: " + file.getName());
            }
        });
    }
}
