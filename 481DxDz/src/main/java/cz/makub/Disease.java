/**
 * Example how to create an ontology in Java code using OWL API.
 *
 * @author Martin Kuba makub@ics.muni.cz
 * modified for cpe481 class project
 */
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
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrdererImpl;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

import java.util.*;
import java.io.*;

/**
 * Example how to use an OWL ontology with a reasoner.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.Tutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class Disease {
    private static File file = new File("Ontology/disease.owl");
    private static final String BASE_URL = "file:///"+file.getAbsolutePath().replace("\\", "/");
    //private static final String BASE_URL = "file:///C:/Users/Ginger/Documents/home/cpe481/Project/481DxDz/Ontology/disease.owl";
    //DL = Description Logic
    private static OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
    
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

        //get class and its individuals
        OWLClass diseaseClass = factory.getOWLClass(":Disease", pm);

        for (OWLNamedIndividual disease : reasoner.getInstances(diseaseClass, false).getFlattened()) {
            System.out.println("disease : " + renderer.render(disease));
        }

        //get a given individual
        OWLNamedIndividual flu = factory.getOWLNamedIndividual(":Flu", pm);

        //get values of selected properties on the individual
        OWLDataProperty includesSymptomProperty = factory.getOWLDataProperty(":includesSymptom", pm);

        //OWLObjectProperty isEmployedAtProperty = factory.getOWLObjectProperty(":isEmployedAt", pm);

        for (OWLLiteral fluSymptom : reasoner.getDataPropertyValues(flu, includesSymptomProperty)) {
            System.out.println("Flu includes symptom: " + fluSymptom.getLiteral());
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
        Collection<OWLClassExpression> assertedClasses = EntitySearcher.getTypes(flu, ontology);
        for (OWLClass c : reasoner.getTypes(flu, false).getFlattened()) {
            boolean asserted = assertedClasses.contains(c);
            System.out.println((asserted ? "asserted" : "inferred") + " class for Flu: " + renderer.render(c));
        }

        //list all object property values for the individual
        Multimap<OWLObjectPropertyExpression, OWLIndividual> assertedValues = EntitySearcher.getObjectPropertyValues(flu, ontology);
        for (OWLObjectProperty objProp : ontology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(flu, objProp).getFlattened()) {
                boolean asserted = assertedValues.get(objProp).contains(ind);
                System.out.println((asserted ? "asserted" : "inferred") + " object property for Flu: "
                        + renderer.render(objProp) + " -> " + renderer.render(ind));
            }
        }

        //list all same individuals
        for (OWLNamedIndividual ind : reasoner.getSameIndividuals(flu)) {
            System.out.println("same as Flu: " + renderer.render(ind));
        }

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

