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

    // Convert quantity obect to num_unit object
    public static NumUnit convertQuantity(Quantity quantity, Example example, QuantSpan quantSpan) {
        
        NumUnit numUnit = new NumUnit();
        
        List<Integer> num_unit_span = new ArrayList<Integer>();
        num_unit_span.add(quantSpan.start);
        num_unit_span.add(quantSpan.end+1);
        numUnit.num_unit_span = num_unit_span;

        numUnit.num = quantity.value.toString(); // normalized and extracted number
        numUnit.unit = quantity.units.trim(); // extracted unit (not neccessarily the unit span)
        
        return numUnit;
    }

    public static void main(String args[]) throws Throwable {

        // Read a jsonl file as list of example objects
        File inputFile = new File(args[0]);        
        File outputFile = new File(args[1]);        

        MappingIterator<Example> iterator = new ObjectMapper().readerFor(Example.class).readValues(inputFile);
        List<Example> examples = iterator.readAll();
        
        Quantifier quantifier = new Quantifier();
        quantifier.doInitialize();
        
        //final File outputFile = new File("data/output.jsonl");

        // Init jsonl writer
        ObjectMapper mapper = new ObjectMapper();
        SequenceWriter seq = mapper.writer()
        .withRootValueSeparator("\n") // Important! Default value separator is single space
        .writeValues(outputFile);

        // Process examples and write to jsonl file
        for (Example e : examples) {
            List<QuantSpan> quantSpans = quantifier.getSpans(e.text, true, null);            
            List<NumUnit> numUnits = new ArrayList<NumUnit>();

            for (QuantSpan q : quantSpans) {

                if (q.object instanceof Quantity) {
                    Quantity quantity = (Quantity) q.object;
                    numUnits.add(convertQuantity(quantity, e, q));
                }                
                if (q.object instanceof Range) {
                    Range range = (Range) q.object;
                    numUnits.add(convertQuantity(range.begins, e, q));
                    numUnits.add(convertQuantity(range.ends, e, q));
                }
                if (q.object instanceof Ratio) {                
                    Ratio ratio = (Ratio) q.object;
                    numUnits.add(convertQuantity(ratio.numerator, e, q));
                    numUnits.add(convertQuantity(ratio.denominator, e, q));
                }
            }
            
            Example prediction = new Example();
            prediction.id = e.id;
            prediction.text = e.text;
            prediction.num_units = numUnits;
            seq.write(prediction);
        }

        
    }
}
