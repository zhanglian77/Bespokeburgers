//Variation on answers from https://stackoverflow.com/questions/7555564/what-is-the-recommended-way-to-make-a-numeric-textfield-in-javafx
//mixed with an answer from https://stackoverflow.com/questions/15159988/javafx-2-2-textfield-maxlength
package client;

import java.util.regex.Pattern;
import java.util.function.UnaryOperator;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;

/**
 * Text field that only accepts integer values as input.
 * Any text pasted into this text field will be stripped of any non-integer characters so that only the integers remain.
 * @see javafx.scene.control.TextField
 * @author Bespoke Burgers
 *
 */
public class IntegerTextField extends TextField {
    private final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]*");
    private final Pattern NON_INTEGER_PATTERN = Pattern.compile("[^\\d]");
    private int maxChars = -1;
    
    /**
     * Creates a TextField with empty text content.
     */
    public IntegerTextField() {
        super();
        setTextFormatter(new TextFormatter<String>(new UnaryOperator<TextFormatter.Change>() {

            @Override
            public Change apply(Change change) {
                if (change.isContentChange()) {
                    String newValue = change.getControlNewText();
                    int newLength = newValue.length();
                    
                    if (!INTEGER_PATTERN.matcher(newValue).matches()) {
                        newValue = NON_INTEGER_PATTERN.matcher(newValue).replaceAll("");
                        newLength = newValue.length();
                    }
                    if (maxChars > 0 && newLength > maxChars) {
                        newValue = newValue.substring(0, maxChars);
                    }
                    change.setText(newValue);
                    change.setRange(0, change.getControlText().length());
                }
                return change;
            }
            
        }));
    }
    
    /**
     * Creates a TextField with initial text content.
     * @param text String: - A string representation of an integer for text content
     * @throws IllegalArgumentException if text is not a representation of an integer
     */
    public IntegerTextField(String text) throws IllegalArgumentException {
        this();
        if (!INTEGER_PATTERN.matcher(text).matches()) throw new IllegalArgumentException("Text must be integer only");
        setText(text);
    }
    
    /**
     * Creates a TextField with initial text content.
     * @param text int: - An integer for text content
     * @throws IllegalArgumentException if maxChars <= 0
     */
    public IntegerTextField(int maxChars) throws IllegalArgumentException {
        this();
        if (maxChars <= 0) throw new IllegalArgumentException("maxChars must be a positive integer");
        this.maxChars = maxChars;
    }
}
