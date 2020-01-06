package af.asr.swift.mt.message;

import com.prowidesoftware.swift.model.field.*;
import com.prowidesoftware.swift.model.mt.mt7xx.MT700;
import com.prowidesoftware.swift.model.mt.mt7xx.MT798;


public class MessageCreator {

    /**
     * This method creates a new MT798 using MT and Field helper classes.
     */
    public static void createMT798(String message) throws Exception {
        /*
         * Create the MT class, it will be initialized as an outgoing message
         * with normal priority
         */
        final MT798 m = new MT798();

        /*
         * Set sender and receiver BIC codes
         */
        m.setSender("FOOSEDR0AXXX");
        m.setReceiver("FOORECV0XXXX");

        /*
         * Start adding the message's fields in correct order
         * This fields are part of the n98 specification
         */
        m.addField(new Field20("FOOI102794-02"));
        m.addField(new Field12("700"));
        m.addField(new Field77E(""));

        /*
         * Proprietary message goes here.
         *
         * This usually involves attaching the text block of another messages.
         * Fields can be individually appended here without restrictions, or
         * the complete text block may be added.
         */
        MT700 mt700 = new MT700();
        mt700.addField(new Field27("2/2"));
        mt700.addField(new Field21A("FOOBAR"));

        m.append(mt700.getSwiftMessage().getBlock4());

        /*
         * Create and print out the SWIFT FIN message string
         */
        System.out.println(m.message());
    }
}
