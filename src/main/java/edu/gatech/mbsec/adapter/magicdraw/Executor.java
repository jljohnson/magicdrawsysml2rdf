package edu.gatech.mbsec.adapter.magicdraw;

import edu.gatech.mbsec.adapter.magicdraw.builder.MetaInformation;
import edu.gatech.mbsec.adapter.magicdraw.builder.Vocabularies;
import edu.gatech.mbsec.adapter.magicdraw.builder.ModelDescriptor;
import com.hp.hpl.jena.rdf.model.Model;
import edu.gatech.mbsec.adapter.magicdraw.builder.OSLC4JMagicDrawApplication;
import edu.gatech.mbsec.adapter.magicdraw.util.OSLCVocabularyCustomizer;
import edu.gatech.mbsec.adapter.magicdraw.writer.FileModelWriter;
import edu.gatech.mbsec.adapter.magicdraw.writer.HttpModelWriter;
import edu.gatech.mbsec.adapter.magicdraw.writer.ModelWriter;
import edu.gatech.mbsec.adapter.magicdraw.writer.StreamModelWriter;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.openjena.riot.Lang;

/**
 * Application's command executor.
 * @author rherrera
 */
public class Executor {
    /**
     * Logger of this class.
     */
    private static final Logger LOG = Logger.getLogger(Executor.class.getName());
    /**
     * Determines whether MagicDraw libreries have been loaded.
     * @return {@code true} if MD libraries are available; {@code false}
     * otherwise.
     */
    private static boolean isMDLoaded() {
        try {
            Class.forName("com.nomagic.runtime.ApplicationExitedException");
            return true;
        } catch(ClassNotFoundException ex) {
            return false;
        }
    }
    /**
     * Gets the available RDF languages.
     * @param toLowerCase indicates whether names should be on lowercase.
     * @return available RDF languages.
     */
    public static Set<String> getAvailableRDFLanguages(boolean toLowerCase) {
        Lang[] values = Lang.values();
        Set<String> availables = new HashSet<>();
        for (Lang language : values) {
            if (toLowerCase)
                availables.add(language.getName().toLowerCase());
            else
                availables.add(language.getName());
        }
        return availables;
    }
    /**
     * Parses the input file into an RDF model.
     * @param mdzip the input file path and name.
     * @param descriptor the building model descriptor.
     * @return the corresponding RDF model.
     * @throws Exception if something goes wrong.
     */
    private static Model getModel(String mdzip, ModelDescriptor descriptor,
            MetaInformation meta) throws Exception {
        Model model = OSLC4JMagicDrawApplication.run(mdzip, descriptor);
        if (meta != null) meta.customize(model, descriptor);
        return model;
    }
    /**
     * Determines the target language from the command line.
     * @param command the execution command.
     * @return the chosen/target language.
     */
    private static Lang getLanguage(CommandLine command) {
        Lang language = Lang.TURTLE;
        String format = command.getOptionValue(Args.format.name());
        if (format != null)
            language = Lang.get(format);
        return language;
    }
    /**
     * Gets the {@link MetaInformation} for this execution.
     * @param command the execution command line.
     * @param descriptor the building descriptor.
     * @return the version control instance.
     */
    private static MetaInformation getMetaInformation(CommandLine command,
            ModelDescriptor descriptor) {
        String[] prefixes = command.getOptionValues(Args.nsprefix.name());
        Properties vocabularies = Vocabularies.asProperties(prefixes);
        String[] meta = command.getOptionValues(Args.meta.name());
        vocabularies.setProperty(descriptor.getVocabPrefix(), descriptor.getVocabBaseURI());
        return MetaInformation.getInstance(vocabularies, meta);
    }
    /**
     * Executes the command given from console.
     * @param command the command to execute.
     * @throws Exception if something goes wrong.
     */
    public static void execute(CommandLine command) throws Exception {
        Model model;
        URL remoteTost;
        ModelWriter writer;
        MetaInformation meta;
        ModelDescriptor descriptor;
        OSLCVocabularyCustomizer customizer;
        ByteArrayOutputStream buffer = null;
        Lang language = getLanguage(command);
        String mdzipFile = command.getOptionValue(Args.mdzip.name());
        String target = command.getOptionValue(Args.target.name());
        String baseURI = command.getOptionValue(Args.base.name());
        String restPath = command.getOptionValue(Args.rest.name());
        String vocabPath = command.getOptionValue(Args.vocab.name());
        if (target == null) {
            buffer = new ByteArrayOutputStream();
            descriptor = new ModelDescriptor(baseURI, restPath, vocabPath);
            meta = getMetaInformation(command, descriptor);
            writer = new StreamModelWriter(buffer);
        } else {
            if (target.startsWith("http")) {
                remoteTost = new URL(target);
                descriptor = new ModelDescriptor(remoteTost, restPath, vocabPath);
                meta = getMetaInformation(command, descriptor);
                writer = new HttpModelWriter(remoteTost, meta.getID());
            } else {
                descriptor = new ModelDescriptor(baseURI, restPath, vocabPath);
                meta = getMetaInformation(command, descriptor);
                writer = new FileModelWriter(target);
            }
        }
        customizer = new OSLCVocabularyCustomizer(descriptor.getVocabBaseURI(), "getRdfTypes");
        customizer.customize("edu.gatech.mbsec.adapter.magicdraw.resources");
        model = getModel(mdzipFile, descriptor, meta);
        writer.write(model, language);
        if (buffer != null) {
            LOG.info(buffer.toString("UTF-8"));
        }
    }
    /**
     * Finishes the execution of this application.
     */
    public static void finish() {
        if (isMDLoaded()) {
            OSLC4JMagicDrawApplication.finish();
        }
    }
}