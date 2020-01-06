package af.asr.swift.mt.parser;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.utils.Lib;

/**
 * This example shows how to read a SWIFT MT message from a file, in the context where
 * the message type to parse is already known, in the example we use an MT 103.
 *
 * Running this program produces the following output:
 <pre>
 Sender: ESPBESMMAXXX
 Receiver: BICFOOYYAXXX
 Sender's Reference: 0061350113089908
 Value Date: 2006/10/28
 Amount: EUR 100000,
 </pre>
 *
 */
public class MT103FromFileParser {

    public static MT103 parse(String path) throws IOException {
        /*
         * Read and parse the file content into a SWIFT message object
         * Parse from File could also be used here
         */
        MT103 mt = MT103.parse(Lib.readResource(path, null));

        /*
         * Print header information
         */
//        System.out.println("Sender: "+mt.getSender());
//        System.out.println("Receiver: "+mt.getReceiver());

        /*
         * Print details of a specific fields
         */
//        Field20 ref = mt.getField20();
//        System.out.println(Field.getLabel(ref.getName(), mt.getMessageType(), null) + ": " + ref.getComponent(Field20.REFERENCE));
//
//        Field32A f = mt.getField32A();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
//        System.out.println("Value Date: "+sdf.format(f.getDateAsCalendar().getTime()));
//        System.out.println("Amount: "+f.getCurrency()+" "+f.getAmount());
        return mt;
    }
}