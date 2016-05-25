/**
 * Example how to create an ontology in Java code using OWL API.
 *
 * @author Martin Kuba makub@ics.muni.cz
 * modified for cpe481 class project
 */
// TODO: change package/folders and keep things working properly.
package cz.makub;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.util.SilentExplanationProgressMonitor;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Multimap;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrdererImpl;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

/**
 * Example how to use an OWL ontology with a reasoner.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.Tutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
// TODO: Make comments easier to read
public class Disease {
    private static final String BASE_URL = "http://users.csc.calpoly.edu/~eyang03/CPE481-KB/481DxDz/Ontology/Disease.owl";
    //DL = Description Logic
    private static OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
    // diseaseSymptoms = disease -> # of symptoms triggering disease
    private static HashMap<String, Double> diseaseSymptoms;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static LinkedList<Map.Entry<String, Double>> sortDiseases() {
    	LinkedList<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(diseaseSymptoms.entrySet());
        Collections.sort(list, new Comparator() {
             public int compare(Object o1, Object o2) {
                return ((Map.Entry<String, Double>)o2).getValue().compareTo(((Map.Entry<String, Double>)o1).getValue());
             }
        });
        return list;
    }
    
    // TODO: cleanup code (remove unneeded comments)
    public static void main(String[] args) throws OWLOntologyCreationException {
        System.out.println(BASE_URL);
        //prepare ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(BASE_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixDocumentFormat pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        pm.setDefaultPrefix(BASE_URL + "#");
        HashMap<String, OWLNamedIndividual> symptomDB = new HashMap<String, OWLNamedIndividual>();
        Scanner scan = new Scanner(System.in);
        boolean selected[];
        
        //get class and its individuals
        OWLClass diseaseClass = factory.getOWLClass(":Disease", pm);
        OWLClass symptomClass = factory.getOWLClass(":Symptom", pm);
        OWLClass patientClass = factory.getOWLClass(":Patient", pm);
        
        // create local symptom db for prompt
        for (OWLNamedIndividual symptom : reasoner.getInstances(symptomClass, false).getFlattened()) {
            symptomDB.put(renderer.render(symptom), symptom);
        }
        
        // prompt user for disease. Do we ask for multiple people, or just run them once?
        // should we just keep it single user?
        String[] symptoms =symptomDB.keySet().toArray(new String[0]);
        selected = new boolean[symptomDB.size()];
        
        int longest = 0;
        for (String str: symptoms)
        	if (longest < str.length())
        		longest = str.length();
		longest++;
        
        String parse;
        do {
        	for (int i = 0; i < symptoms.length; i++) {
        		if ((i != 0 && symptoms.length < 10) || (i % 3 == 0 && i != 0))
        			System.out.println();
        		int maxIntLength = (new String(""+symptoms.length)).length();
        		String index = new String("" + (i+1));
        		System.out.print("[" + (selected[i]? "x" : " ") + "] " +  String.format("%"+maxIntLength+"s", index) + ". " + symptoms[i].replace("_", " ") +  String.format("%" + (longest-symptoms[i].length()) + "s"," ") + " ");
        	}
        	System.out.print("\nPlease select symptoms (press <ENTER> to continue): ");
        	parse = scan.nextLine().trim();
        	if (parse.length() == 0) break;
    		String[] parsed = parse.split(",");
    		for (int i = 0; i < parsed.length; i++) {
    			// no type checking
    			selected[Integer.parseInt(parsed[i].trim())-1] = true;
    		}
        } while (true);
        
        OWLNamedIndividual patient0 = factory.getOWLNamedIndividual(":patient0", pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(patient0));
        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(patientClass, patient0));
        OWLObjectProperty hasSymptoms = factory.getOWLObjectProperty(":hasSymptoms", pm);
        
        // transfer symptoms into ontology
		for (int i = 0; i < symptoms.length; i++) {
			if (selected[i]) {
//				System.out.print(symptoms[i]);
				manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(hasSymptoms, patient0, symptomDB.get(symptoms[i])));
//				System.out.println();
			}
		}

		reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
		
//        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(patient0, hasSymptoms).getFlattened()) {
//            System.out.println(renderer.render(ind));
//        }
		
        // run all individuals
		// TODO: clean up strings to sound more user friendly
        for (OWLNamedIndividual person : reasoner.getInstances(patientClass, false).getFlattened()) {
        	diseaseSymptoms = new HashMap<String, Double>();
        	//OWLNamedIndividual person = factory.getOWLNamedIndividual(":Patient0", pm);
	        // get values of selected properties on the individual
	        OWLObjectProperty hasDisease = factory.getOWLObjectProperty(":hasDisease", pm);
	        // put all diseases in a map. 
	        for (OWLNamedIndividual disease : reasoner.getObjectPropertyValues(person, hasDisease).getFlattened()) {
	        	diseaseSymptoms.put(renderer.render(disease),0.0);
	        }
	
	        String[] diseases = diseaseSymptoms.keySet().toArray(new String[0]);
	        
	        for (OWLObjectProperty objProp : ontology.getObjectPropertiesInSignature(Imports.INCLUDED))
	            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(person, objProp).getFlattened())
	                for (String disease: diseases)
	                	if (renderer.render(objProp).toLowerCase().contains(disease.toLowerCase()))
	                		diseaseSymptoms.put(disease, diseaseSymptoms.get(disease)+1);
	        
	        // get max length of diseases + convert symptom count to fraction of total symptoms for disease
	        int max = 0;
	        for (String disease : diseaseSymptoms.keySet()) {
	        	// update max length of diseases if needed
	        	if (disease.length() > max)
	        		max = disease.length();
	        	OWLNamedIndividual curDisease = factory.getOWLNamedIndividual(":"+disease, pm);
	        	OWLObjectProperty includesSymptom = factory.getOWLObjectProperty(":includesSymptom", pm);
	        	int numSymptoms = reasoner.getObjectPropertyValues(curDisease, includesSymptom).getFlattened().size();
	        	diseaseSymptoms.put(disease, diseaseSymptoms.get(disease)/numSymptoms);
	        }
	        
	        System.out.println("The diseases that " + renderer.render(person) + " may have are: (ordered by likeliness)");
	        for (Map.Entry<String, Double> entry : sortDiseases())
	        	System.out.println(String.format("\t%"+max+"s\t%.3f", entry.getKey(), entry.getValue()));
        }
        /*
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin, isEmployedAtProperty).getFlattened()) {
            System.out.println("Martin is employed at: " + renderer.render(ind));
        }
        */

        /* this is for data properties - not any in our ontology
        //get labels
        LocalizedAnnotationSelector as = new LocalizedAnnotationSelector(ontology, factory, "en", "cs");
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(flu, includesSymptomProperty).getFlattened()) {
            System.out.println("Flu includes symptom (label): '" + as.getLabel(ind) + "'");
        }
        */

        /*
        //get inverse of a property, i.e. which individuals are in relation with a given individual
        OWLNamedIndividual university = factory.getOWLNamedIndividual(":MU", pm);
        OWLObjectPropertyExpression inverse = factory.getOWLObjectInverseOf(isEmployedAtProperty);
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(university, inverse).getFlattened()) {
            System.out.println("MU inverseOf(isEmployedAt) -> " + renderer.render(ind));
        }
        */

        //find to which classes the individual belongs
//        Collection<OWLClassExpression> assertedClasses = EntitySearcher.getTypes(person, ontology);
//        for (OWLClass c : reasoner.getTypes(person, false).getFlattened()) {
//            boolean asserted = assertedClasses.contains(c);
//            System.out.println((asserted ? "asserted" : "inferred") + " class for Flu: " + renderer.render(c));
//        }

//        //list all object property values for the individual
//        Multimap<OWLObjectPropertyExpression, OWLIndividual> assertedValues = EntitySearcher.getObjectPropertyValues(person, ontology);
//        for (OWLObjectProperty objProp : ontology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
//            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(person, objProp).getFlattened()) {
//                boolean asserted = assertedValues.get(objProp).contains(ind);
//                System.out.println((asserted ? "asserted" : "inferred") + " object property for Flu: "
//                        + renderer.render(objProp) + " -> " + renderer.render(ind));
//            }
//        }

        //list all same individuals
//        for (OWLNamedIndividual ind : reasoner.getSameIndividuals(person)) {
//            System.out.println("same as Flu: " + renderer.render(ind));
//        }

        /*TODO
        //ask reasoner whether Flu includes symptom Fever
        boolean result = reasoner.isEntailed(factory.getOWLObjectPropertyAssertionAxiom(includesSymptomProperty, flu, university));
        System.out.println("Is Martin employed at MU ? : " + result);


        //check whether the SWRL rule is used
        OWLNamedIndividual ivan = factory.getOWLNamedIndividual(":Ivan", pm);
        OWLClass chOMPClass = factory.getOWLClass(":ChildOfMarriedParents", pm);
        OWLClassAssertionAxiom axiomToExplain = factory.getOWLClassAssertionAxiom(chOMPClass, ivan);
        System.out.println("Is Ivan child of married parents ? : " + reasoner.isEntailed(axiomToExplain));


        //explain why Ivan is child of married parents
        DefaultExplanationGenerator explanationGenerator =
                new DefaultExplanationGenerator(
                        manager, reasonerFactory, ontology, reasoner, new SilentExplanationProgressMonitor());
        Set<OWLAxiom> explanation = explanationGenerator.getExplanation(axiomToExplain);
        ExplanationOrderer deo = new ExplanationOrdererImpl(manager);
        ExplanationTree explanationTree = deo.getOrderedExplanation(axiomToExplain, explanation);
        System.out.println();
        System.out.println("-- explanation why Ivan is in class ChildOfMarriedParents --");
        printIndented(explanationTree, "");
        */
    }

    private static void printIndented(Tree<OWLAxiom> node, String indent) {
        OWLAxiom axiom = node.getUserObject();
        System.out.println(indent + renderer.render(axiom));
        if (!node.isLeaf()) {
            for (Tree<OWLAxiom> child : node.getChildren()) {
                printIndented(child, indent + "    ");
            }
        }
    }

    
    /**
     * Helper class for extracting labels, comments and other annotations in preferred languages.
     * Selects the first literal annotation matching the given languages in the given order.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LocalizedAnnotationSelector {
        private final List<String> langs;
        private final OWLOntology ontology;
        private final OWLDataFactory factory;

        /**
         * Constructor.
         *
         * @param ontology ontology
         * @param factory  data factory
         * @param langs    list of preferred languages; if none is provided the Locale.getDefault() is used
         */
        public LocalizedAnnotationSelector(OWLOntology ontology, OWLDataFactory factory, String... langs) {
            this.langs = (langs == null) ? Collections.singletonList(Locale.getDefault().toString()) : Arrays.asList(langs);
            this.ontology = ontology;
            this.factory = factory;
        }

        /**
         * Provides the first label in the first matching language.
         *
         * @param ind individual
         * @return label in one of preferred languages or null if not available
         */
        public String getLabel(OWLNamedIndividual ind) {
            return getAnnotationString(ind, OWLRDFVocabulary.RDFS_LABEL.getIRI());
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getComment(OWLNamedIndividual ind) {
            return getAnnotationString(ind, OWLRDFVocabulary.RDFS_COMMENT.getIRI());
        }

        public String getAnnotationString(OWLNamedIndividual ind, IRI annotationIRI) {
            return getLocalizedString(EntitySearcher.getAnnotations(ind, ontology, factory.getOWLAnnotationProperty(annotationIRI)));
        }

        private String getLocalizedString(Collection<OWLAnnotation> annotations) {
            List<OWLLiteral> literalLabels = new ArrayList<>(annotations.size());
            for (OWLAnnotation label : annotations) {
                if (label.getValue() instanceof OWLLiteral) {
                    literalLabels.add((OWLLiteral) label.getValue());
                }
            }
            for (String lang : langs) {
                for (OWLLiteral literal : literalLabels) {
                    if (literal.hasLang(lang)) return literal.getLiteral();
                }
            }
            for (OWLLiteral literal : literalLabels) {
                if (!literal.hasLang()) return literal.getLiteral();
            }
            return null;
        }
    }
}
