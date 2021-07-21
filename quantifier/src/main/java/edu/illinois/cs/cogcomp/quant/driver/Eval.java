/**
 * This software is released under the University of Illinois/Research and Academic Use License. See
 * the LICENSE file in the root folder for details. Copyright (c) 2016
 *
 * Developed by: The Cognitive Computation Group University of Illinois at Urbana-Champaign
 * http://cogcomp.org/
 */
package edu.illinois.cs.cogcomp.quant.driver;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.quant.lbj.*;
import edu.illinois.cs.cogcomp.quant.standardize.Normalizer;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.learn.BatchTrainer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.quant.standardize.*;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public class Eval {
    

    public static void main(String args[]) throws Throwable {

        // Read a jsonl file as list of example objects
        File inputFile = new File(args[0]);        
        MappingIterator<Example> iterator = new ObjectMapper().readerFor(Example.class).readValues(inputFile);
        List<Example> examples = iterator.readAll();
        
        Quantifier quantifier = new Quantifier();
        quantifier.doInitialize();
        
        final File outputFile = new File("data/data.ldjson");

        /* ObjectMapper mapper = new ObjectMapper();
        try (SequenceWriter seq = mapper.writer()
        .withRootValueSeparator("\n") // Important! Default value separator is single space
        .writeValues(outputFile)) {
            IdValue value = ...;
            seq.write(value);
        } */

        ObjectMapper mapper = new ObjectMapper();
        SequenceWriter seq = mapper.writer()
        .withRootValueSeparator("\n") // Important! Default value separator is single space
        .writeValues(outputFile);

        // ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        // Process examples and write to jsonl file
        for (Example e : examples) {
            List<QuantSpan> quant_spans = quantifier.getSpans(e.text, true, null);
            
            List<NumUnit> num_units = new ArrayList<NumUnit>();
            for (QuantSpan q : quant_spans) {
                if (q.object instanceof Quantity) {
                    Quantity quantity = (Quantity) q.object;
                    NumUnit num_unit = new NumUnit();
                    num_unit.num = quantity.value;
                    num_unit.unit = quantity.units.trim();
                    num_unit.unit_span = quantity.unit_span.trim();
                    num_unit.num_unit_span = quantity.phrase.trim();

                    num_unit.num_span = num_unit.num_unit_span.replace(num_unit.unit_span, "").trim();

                    num_units.add(num_unit);
                }                
            }
            
            Example prediction = new Example();
            prediction.id = e.id;
            prediction.text = e.text;
            prediction.num_units = num_units;

            // System.out.println(quant_spans);
            // currently overwrites previous example
            seq.write(prediction);
        }

        
    }
}
